package com.streaming.events.exception;

import com.streaming.events.enums.EventStatus;
import com.streaming.events.model.*;
import com.streaming.events.serde.JsonEventDeserializer;
import com.streaming.events.serde.JsonEventSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes do Fluxo de Exceção e Resiliência.
 * Verifica que a hierarquia de exceções, a semântica de retentativa
 * e o encaminhamento para DLQ funcionam conforme projetados.
 */
@DisplayName("Fluxo de Exceção e Resiliência")
class ExceptionFlowTest {

    // =========================================================================
    // EventValidationException
    // =========================================================================

    @Nested
    @DisplayName("EventValidationException (erro fatal / não-retentável)")
    class EventValidationExceptionTest {

        @Test
        @DisplayName("deve ser marcada como não-retentável (vai direto para DLQ)")
        void deveSerNaoRetentavel() {
            var eventId = UUID.randomUUID();
            var violations = List.of("campo 'source' é obrigatório", "campo 'eventType' inválido");

            var exception = new EventValidationException(eventId, violations);

            assertThat(exception.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("deve armazenar o eventId para correlação com a DLQ")
        void deveArmazenarEventId() {
            var eventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            var exception = new EventValidationException(eventId, List.of("erro"));

            assertThat(exception.getEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("deve armazenar lista de violações de forma imutável")
        void deveArmazenarViolacoes() {
            var violations = List.of("campo obrigatório", "formato inválido");
            var exception = new EventValidationException(UUID.randomUUID(), violations);

            assertThat(exception.getViolations()).hasSize(2);
            assertThat(exception.getViolations()).containsExactly("campo obrigatório", "formato inválido");
        }

        @Test
        @DisplayName("violações devem ser imutáveis (cópia defensiva)")
        void violacoesDevemSerImutaveis() {
            var exception = new EventValidationException(UUID.randomUUID(), List.of("erro"));

            assertThatThrownBy(() -> exception.getViolations().add("nova violação"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("mensagem deve conter as violações concatenadas")
        void mensagemDeveConterViolacoes() {
            var violations = List.of("campo A inválido", "campo B ausente");
            var exception = new EventValidationException(UUID.randomUUID(), violations);

            assertThat(exception.getMessage()).contains("campo A inválido");
            assertThat(exception.getMessage()).contains("campo B ausente");
        }

        @Test
        @DisplayName("deve herdar de EventProcessingException e RuntimeException")
        void deveHerdarCorretamente() {
            var exception = new EventValidationException(UUID.randomUUID(), List.of("erro"));

            assertThat(exception).isInstanceOf(EventProcessingException.class);
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("lista nula de violações deve resultar em lista vazia")
        void listaNulaDeveSerListaVazia() {
            var exception = new EventValidationException(UUID.randomUUID(), null);

            assertThat(exception.getViolations()).isEmpty();
        }
    }

    // =========================================================================
    // EventDeserializationException (Poison Pill)
    // =========================================================================

    @Nested
    @DisplayName("EventDeserializationException (Poison Pill)")
    class EventDeserializationExceptionTest {

        @Test
        @DisplayName("deve armazenar o tópico de origem")
        void deveArmazenarTopico() {
            var cause = new IOException("parse error");
            var exception = new EventDeserializationException("events.raw.ingest", "{bad json}", "Erro", cause);

            assertThat(exception.getTopic()).isEqualTo("events.raw.ingest");
        }

        @Test
        @DisplayName("deve armazenar o payload bruto para análise posterior (DLQ)")
        void deveArmazenarRawPayload() {
            var rawPayload = "{\"campo\": quebrado}";
            var exception = new EventDeserializationException("topic", rawPayload, "Erro", new IOException());

            assertThat(exception.getRawPayload()).isEqualTo(rawPayload);
        }

        @Test
        @DisplayName("deve preservar a causa original (IOException)")
        void devePreservarCausaOriginal() {
            var originalCause = new IOException("Unexpected end of JSON input");
            var exception = new EventDeserializationException("topic", "payload", "Erro", originalCause);

            assertThat(exception.getCause()).isEqualTo(originalCause);
            assertThat(exception.getCause()).isInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("deve herdar de RuntimeException (erro não-verificado)")
        void deveHerdarDeRuntimeException() {
            var exception = new EventDeserializationException("topic", "payload", "Erro", new IOException());

            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("NÃO deve herdar de EventProcessingException (é exceção de infraestrutura)")
        void naoDeveHerdarDeEventProcessingException() {
            var exception = new EventDeserializationException("topic", "payload", "Erro", new IOException());

            assertThat(exception).isNotInstanceOf(EventProcessingException.class);
        }
    }

    // =========================================================================
    // Fluxo completo: Desserialização → Exceção → DLQ
    // =========================================================================

    @Nested
    @DisplayName("Fluxo de Poison Pill: Deserializer → Exception → DlqEvent")
    class FluxoPoisonPillCompleto {

        @Test
        @DisplayName("JSON malformado deve gerar EventDeserializationException que pode ser encapsulada em DlqEvent")
        void jsonMalformadoDeveGerarDlqEvent() {
            var deserializer = new JsonEventDeserializer<>(RawEvent.class);
            String invalidJson = "{ not valid json }";
            byte[] bytes = invalidJson.getBytes(StandardCharsets.UTF_8);

            try {
                deserializer.deserialize("events.raw.ingest", bytes);
            } catch (EventDeserializationException ex) {
                // Simula o encaminhamento para DLQ com informações da exceção
                var dlqEvent = DlqEvent.create(
                        Map.of("rawPayload", ex.getRawPayload()),
                        ex.getTopic(),
                        ex.getMessage(),
                        getStackTraceAsString(ex),
                        0  // Poison Pill: 0 retentativas
                );

                assertThat(dlqEvent.originTopic()).isEqualTo("events.raw.ingest");
                assertThat(dlqEvent.retryCount()).isZero();
                assertThat(dlqEvent.originalEvent()).containsEntry("rawPayload", invalidJson);
                assertThat(dlqEvent.errorMessage()).contains("events.raw.ingest");
                return;
            }

            throw new AssertionError("Deveria ter lançado EventDeserializationException");
        }

        @Test
        @DisplayName("validação falha deve gerar EventValidationException que pode ser encapsulada em DlqEvent")
        void validacaoFalhaDeveGerarDlqEvent() {
            var eventId = UUID.randomUUID();
            var violations = List.of("source não pode ser vazio", "payload inválido");
            var validationEx = new EventValidationException(eventId, violations);

            // Simula criação de DlqEvent a partir da exceção de validação
            var dlqEvent = DlqEvent.create(
                    Map.of("eventId", eventId.toString(), "violations", violations.toString()),
                    "events.raw.ingest",
                    validationEx.getMessage(),
                    getStackTraceAsString(validationEx),
                    0  // Não-retentável: 0 retentativas
            );

            assertThat(dlqEvent.retryCount()).isZero();
            assertThat(dlqEvent.errorMessage()).contains("validação");
            assertThat(dlqEvent.originalEvent()).containsKey("eventId");
        }
    }

    // =========================================================================
    // Semântica de retentativa (retryable vs non-retryable)
    // =========================================================================

    @Nested
    @DisplayName("Semântica de retentativa")
    class SemanticaRetentativa {

        @Test
        @DisplayName("EventValidationException deve ser não-retentável (false)")
        void validacaoDeveSerNaoRetentavel() {
            var exception = new EventValidationException(UUID.randomUUID(), List.of("erro"));

            assertThat(exception.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("exceções retentáveis devem ter isRetryable=true")
        void excecoesRetentaveisDevemTerFlagTrue() {
            // Cria uma subclasse concreta simulando um erro transitório
            var retryableException = new EventProcessingException(UUID.randomUUID(), "Timeout no broker", true) {};

            assertThat(retryableException.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("exceções retentáveis devem preservar o eventId")
        void excecoesRetentaveisDevemPreservarEventId() {
            var eventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            var retryableException = new EventProcessingException(eventId, "Timeout", true) {};

            assertThat(retryableException.getEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("exceção com causa deve preservar a cadeia de exceções")
        void excecaoComCausaDevePreservarCadeia() {
            var cause = new IOException("Connection reset");
            var eventId = UUID.randomUUID();
            var exception = new EventProcessingException(eventId, "Erro de comunicação", cause, true) {};

            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getMessage()).isEqualTo("Erro de comunicação");
            assertThat(exception.isRetryable()).isTrue();
        }
    }

    // =========================================================================
    // Status do ciclo de vida em cenários de exceção
    // =========================================================================

    @Nested
    @DisplayName("Status do ciclo de vida em cenários de exceção")
    class StatusCicloDeVidaExcecao {

        @Test
        @DisplayName("ERROR_RETRYING não deve ser terminal (evento ainda em processamento)")
        void errorRetryingNaoDeveSerTerminal() {
            assertThat(EventStatus.ERROR_RETRYING.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("DEAD_LETTER deve ser terminal (fim do ciclo)")
        void deadLetterDeveSerTerminal() {
            assertThat(EventStatus.DEAD_LETTER.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("REJECTED deve ser terminal (erro fatal de negócio)")
        void rejectedDeveSerTerminal() {
            assertThat(EventStatus.REJECTED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("RECOVERED deve ser terminal (reprocessado com sucesso)")
        void recoveredDeveSerTerminal() {
            assertThat(EventStatus.RECOVERED.isTerminal()).isTrue();
        }
    }

    // =========================================================================
    // DlqEvent: integridade e serialização
    // =========================================================================

    @Nested
    @DisplayName("DlqEvent - integridade na serialização e rastreabilidade")
    class DlqEventResiliencia {

        @Test
        @DisplayName("DlqEvent deve ser serializável e desserializável (roundtrip)")
        void dlqEventDeveSerSerializavel() {
            var serializer = new JsonEventSerializer<DlqEvent>();
            var deserializer = new JsonEventDeserializer<>(DlqEvent.class);

            var dlq = DlqEvent.create(
                    Map.of("eventId", "abc-123", "source", "test"),
                    "events.raw.ingest",
                    "Timeout no processamento",
                    "java.net.SocketTimeoutException at...",
                    3
            );

            byte[] bytes = serializer.serialize("events.dlq", dlq);
            DlqEvent deserialized = deserializer.deserialize("events.dlq", bytes);

            assertThat(deserialized.dlqId()).isEqualTo(dlq.dlqId());
            assertThat(deserialized.originTopic()).isEqualTo("events.raw.ingest");
            assertThat(deserialized.errorMessage()).isEqualTo("Timeout no processamento");
            assertThat(deserialized.retryCount()).isEqualTo(3);
            assertThat(deserialized.originalEvent()).containsEntry("eventId", "abc-123");
        }

        @Test
        @DisplayName("DlqEvent deve aceitar stackTrace nulo (para erros sem stack trace)")
        void dlqEventDeveAceitarStackTraceNulo() {
            var dlq = DlqEvent.create(Map.of("k", "v"), "topic", "Erro fatal", null, 0);

            assertThat(dlq.stackTrace()).isNull();
            assertThat(dlq.retryCount()).isZero();
        }
    }

    private String getStackTraceAsString(Exception ex) {
        var sw = new java.io.StringWriter();
        ex.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
}