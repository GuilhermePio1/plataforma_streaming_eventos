package com.streaming.events.model;

import com.streaming.events.enums.EventPriority;
import com.streaming.events.enums.EventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProcessedEvent - Testes de Integridade")
class ProcessedEventTest {

    private final EventHeader header = EventHeader.create("src", "type");

    @Nested
    @DisplayName("Construtor compacto (validações)")
    class ConstrutorCompacto {

        @Test
        @DisplayName("deve criar ProcessedEvent com campos válidos")
        void deveCriarProcessedEventValido() {
            var payload = Map.<String, Object>of("data", "value");
            var result = Map.<String, Object>of("agg", "sum");
            var now = Instant.now();

            var event = new ProcessedEvent(header, EventPriority.NORMAL, EventStatus.PROCESSED, payload, result, now, "sha256-abc");

            assertThat(event.header()).isEqualTo(header);
            assertThat(event.status()).isEqualTo(EventStatus.PROCESSED);
            assertThat(event.payload()).containsEntry("data", "value");
            assertThat(event.result()).containsEntry("agg", "sum");
            assertThat(event.payloadHash()).isEqualTo("sha256-abc");
        }

        @Test
        @DisplayName("deve rejeitar header nulo")
        void deveRejeitarHeaderNulo() {
            assertThatThrownBy(() -> new ProcessedEvent(null, EventPriority.NORMAL, EventStatus.PROCESSED, Map.of(), Map.of(), Instant.now(), "hash"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("deve rejeitar status nulo")
        void deveRejeitarStatusNulo() {
            assertThatThrownBy(() -> new ProcessedEvent(header, EventPriority.NORMAL, null, Map.of(), Map.of(), Instant.now(), "hash"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("deve rejeitar payloadHash nulo")
        void deveRejeitarPayloadHashNulo() {
            assertThatThrownBy(() -> new ProcessedEvent(header, EventPriority.NORMAL, EventStatus.PROCESSED, Map.of(), Map.of(), Instant.now(), null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("deve rejeitar payloadHash vazio")
        void deveRejeitarPayloadHashVazio() {
            assertThatThrownBy(() -> new ProcessedEvent(header, EventPriority.NORMAL, EventStatus.PROCESSED, Map.of(), Map.of(), Instant.now(), ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("O hash do payload não pode ser vazio"); // <-- Garantia final
        }

        @Test
        @DisplayName("deve rejeitar payloadHash com apenas espaços")
        void deveRejeitarPayloadHashEmBranco() {
            assertThatThrownBy(() -> new ProcessedEvent(header, EventPriority.NORMAL, EventStatus.PROCESSED, Map.of(), Map.of(), Instant.now(), "   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Imutabilidade")
    class Imutabilidade {

        @Test
        @DisplayName("payload e result devem ser imutáveis")
        void payloadEResultDevemSerImutaveis() {
            var event = new ProcessedEvent(header, EventPriority.NORMAL, EventStatus.PROCESSED, Map.of("k", "v"), Map.of("r", "v"), Instant.now(), "hash");

            assertThatThrownBy(() -> event.payload().put("nova", "entrada"))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> event.result().put("nova", "entrada"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("result nulo deve resultar em mapa vazio")
        void resultNuloDeveSerMapaVazio() {
            var event = new ProcessedEvent(header, EventPriority.NORMAL, EventStatus.PROCESSED, Map.of("k", "v"), null, Instant.now(), "hash");

            assertThat(event.result()).isEmpty();
        }

        @Test
        @DisplayName("mutação no mapa original não deve afetar o evento")
        void mutacaoExternaNaoAfetaEvento() {
            var mutablePayload = new HashMap<String, Object>();
            mutablePayload.put("key", "original");
            var mutableResult = new HashMap<String, Object>();
            mutableResult.put("res", "original");

            var event = new ProcessedEvent(header, EventPriority.NORMAL, EventStatus.PROCESSED, mutablePayload, mutableResult, Instant.now(), "hash");

            mutablePayload.put("key", "modificado");
            mutableResult.put("res", "modificado");

            assertThat(event.payload()).containsEntry("key", "original");
            assertThat(event.result()).containsEntry("res", "original");
        }
    }

    @Nested
    @DisplayName("Factory method from()")
    class FactoryMethod {

        @Test
        @DisplayName("deve criar ProcessedEvent a partir de ValidatedEvent com status PROCESSED")
        void deveCriarAPartirDeValidatedEvent() {
            var raw = RawEvent.create("src", "type", EventPriority.HIGH, Map.of("data", "val"));
            var validated = ValidatedEvent.from(raw, "1.0");
            var result = Map.<String, Object>of("count", 42);

            var processed = ProcessedEvent.from(validated, result, "sha256-xyz");

            assertThat(processed.header()).isEqualTo(validated.header());
            assertThat(processed.priority()).isEqualTo(EventPriority.HIGH);
            assertThat(processed.status()).isEqualTo(EventStatus.PROCESSED);
            assertThat(processed.payload()).isEqualTo(validated.payload());
            assertThat(processed.result()).containsEntry("count", 42);
            assertThat(processed.payloadHash()).isEqualTo("sha256-xyz");
            assertThat(processed.processedAt()).isNotNull();
        }
    }
}