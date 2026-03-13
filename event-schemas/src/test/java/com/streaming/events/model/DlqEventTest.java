package com.streaming.events.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DlqEvent - Testes de Integridade")
class DlqEventTest {

    @Nested
    @DisplayName("Construtor compacto (validações)")
    class ConstrutorCompacto {

        @Test
        @DisplayName("deve criar DlqEvent com campos válidos")
        void deveCriarDlqEventValido() {
            var dlqId = UUID.randomUUID();
            var originalEvent = Map.<String, Object>of("data", "value");
            var now = Instant.now();

            var event = new DlqEvent(dlqId, originalEvent, "events.raw.ingest", "Falha de validação", "stack...", 3, now);

            assertThat(event.dlqId()).isEqualTo(dlqId);
            assertThat(event.originalEvent()).containsEntry("data", "value");
            assertThat(event.originTopic()).isEqualTo("events.raw.ingest");
            assertThat(event.errorMessage()).isEqualTo("Falha de validação");
            assertThat(event.stackTrace()).isEqualTo("stack...");
            assertThat(event.retryCount()).isEqualTo(3);
            assertThat(event.failedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("deve rejeitar dlqId nulo")
        void deveRejeitarDlqIdNulo() {
            assertThatThrownBy(() -> new DlqEvent(null, Map.of(), "topic", "msg", null, 0, Instant.now()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("deve rejeitar originalEvent nulo")
        void deveRejeitarOriginalEventNulo() {
            assertThatThrownBy(() -> new DlqEvent(UUID.randomUUID(), null, "topic", "msg", null, 0, Instant.now()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("deve rejeitar originTopic nulo")
        void deveRejeitarOriginTopicNulo() {
            assertThatThrownBy(() -> new DlqEvent(UUID.randomUUID(), Map.of(), null, "msg", null, 0, Instant.now()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("deve rejeitar originTopic vazio")
        void deveRejeitarOriginTopicVazio() {
            assertThatThrownBy(() -> new DlqEvent(UUID.randomUUID(), Map.of(), "", "msg", null, 0, Instant.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("O tópico de origem não pode ser vazio");
        }

        @Test
        @DisplayName("deve rejeitar errorMessage nula")
        void deveRejeitarErrorMessageNula() {
            assertThatThrownBy(() -> new DlqEvent(UUID.randomUUID(), Map.of(), "topic", null, null, 0, Instant.now()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("deve rejeitar errorMessage vazia")
        void deveRejeitarErrorMessageVazia() {
            assertThatThrownBy(() -> new DlqEvent(UUID.randomUUID(), Map.of(), "topic", "", null, 0, Instant.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("A mensagem de erro não pode ser vazia");
        }

        @Test
        @DisplayName("deve rejeitar retryCount negativo")
        void deveRejeitarRetryCountNegativo() {
            assertThatThrownBy(() -> new DlqEvent(UUID.randomUUID(), Map.of(), "topic", "msg", null, -1, Instant.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("O número de tentativas não pode ser negativo");
        }

        @Test
        @DisplayName("deve aceitar retryCount zero (erro fatal sem retentativas)")
        void deveAceitarRetryCountZero() {
            var event = new DlqEvent(UUID.randomUUID(), Map.of("k", "v"), "topic", "fatal error", null, 0, Instant.now());

            assertThat(event.retryCount()).isZero();
        }

        @Test
        @DisplayName("deve rejeitar failedAt nulo")
        void deveRejeitarFailedAtNulo() {
            assertThatThrownBy(() -> new DlqEvent(UUID.randomUUID(), Map.of(), "topic", "msg", null, 0, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Imutabilidade do originalEvent")
    class Imutabilidade {

        @Test
        @DisplayName("originalEvent deve ser imutável após criação")
        void originalEventDeveSerImutavel() {
            var event = DlqEvent.create(Map.of("k", "v"), "topic", "msg", "stack", 1);

            assertThatThrownBy(() -> event.originalEvent().put("nova", "entrada"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("mutação no mapa original não deve afetar o evento")
        void mutacaoExternaNaoAfetaEvento() {
            var mutableMap = new HashMap<String, Object>();
            mutableMap.put("key", "original");

            var event = new DlqEvent(UUID.randomUUID(), mutableMap, "topic", "msg", null, 0, Instant.now());

            mutableMap.put("key", "modificado");
            assertThat(event.originalEvent()).containsEntry("key", "original");
        }
    }

    @Nested
    @DisplayName("Factory method create()")
    class FactoryMethod {

        @Test
        @DisplayName("deve gerar dlqId e failedAt automaticamente")
        void deveGerarCamposAutomaticamente() {
            var event = DlqEvent.create(Map.of("data", "val"), "events.raw.ingest", "Timeout", "java.lang.Exception...", 5);

            assertThat(event.dlqId()).isNotNull();
            assertThat(event.failedAt()).isNotNull();
            assertThat(event.originTopic()).isEqualTo("events.raw.ingest");
            assertThat(event.errorMessage()).isEqualTo("Timeout");
            assertThat(event.stackTrace()).isEqualTo("java.lang.Exception...");
            assertThat(event.retryCount()).isEqualTo(5);
        }
    }
}