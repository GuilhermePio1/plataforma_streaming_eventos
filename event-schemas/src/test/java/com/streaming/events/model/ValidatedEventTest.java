package com.streaming.events.model;

import com.streaming.events.enums.EventPriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ValidatedEvent - Testes de Integridade")
class ValidatedEventTest {

    private final EventHeader header = EventHeader.create("src", "type");

    @Nested
    @DisplayName("Construtor compacto (validações)")
    class ConstrutorCompacto {

        @Test
        @DisplayName("deve criar ValidatedEvent com campos válidos")
        void deveCriarValidatedEventValido() {
            var payload = Map.<String, Object>of("data", "value");
            var now = Instant.now();

            var event = new ValidatedEvent(header, EventPriority.NORMAL, payload, now, "1.0.0");

            assertThat(event.header()).isEqualTo(header);
            assertThat(event.priority()).isEqualTo(EventPriority.NORMAL);
            assertThat(event.payload()).containsEntry("data", "value");
            assertThat(event.validatedAt()).isEqualTo(now);
            assertThat(event.schemaVersion()).isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("deve rejeitar header nulo")
        void deveRejeitarHeaderNulo() {
            assertThatThrownBy(() -> new ValidatedEvent(null, EventPriority.NORMAL, Map.of(), Instant.now(), "1.0"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("deve rejeitar priority nula")
        void deveRejeitarPriorityNula() {
            assertThatThrownBy(() -> new ValidatedEvent(header, null, Map.of(), Instant.now(), "1.0"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("deve rejeitar validatedAt nulo")
        void deveRejeitarValidatedAtNulo() {
            assertThatThrownBy(() -> new ValidatedEvent(header, EventPriority.NORMAL, Map.of(), null, "1.0"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("deve rejeitar schemaVersion nulo")
        void deveRejeitarSchemaVersionNulo() {
            assertThatThrownBy(() -> new ValidatedEvent(header, EventPriority.NORMAL, Map.of(), Instant.now(), null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("deve rejeitar schemaVersion vazio")
        void deveRejeitarSchemaVersionVazio() {
            assertThatThrownBy(() -> new ValidatedEvent(header, EventPriority.NORMAL, Map.of(), Instant.now(), ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("A versão do schema não pode ser vazia"); // <--- Adicionar isso
        }

        @Test
        @DisplayName("deve rejeitar schemaVersion com apenas espaços")
        void deveRejeitarSchemaVersionEmBranco() {
            assertThatThrownBy(() -> new ValidatedEvent(header, EventPriority.NORMAL, Map.of(), Instant.now(), "   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Imutabilidade do payload")
    class Imutabilidade {

        @Test
        @DisplayName("payload deve ser imutável após criação")
        void payloadDeveSerImutavel() {
            var event = new ValidatedEvent(header, EventPriority.NORMAL, Map.of("k", "v"), Instant.now(), "1.0");

            assertThatThrownBy(() -> event.payload().put("nova", "entrada"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("mutação no mapa original não deve afetar o evento")
        void mutacaoExternaNaoAfetaEvento() {
            var mutablePayload = new HashMap<String, Object>();
            mutablePayload.put("key", "original");

            var event = new ValidatedEvent(header, EventPriority.NORMAL, mutablePayload, Instant.now(), "1.0");

            mutablePayload.put("key", "modificado");
            assertThat(event.payload()).containsEntry("key", "original");
        }
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("from() deve criar evento validado a partir de RawEvent")
        void fromDevePreservarDadosDoRawEvent() {
            var raw = RawEvent.create("src", "type", EventPriority.HIGH, Map.of("data", "val"));

            var validated = ValidatedEvent.from(raw, "2.0.0");

            assertThat(validated.header()).isEqualTo(raw.header());
            assertThat(validated.priority()).isEqualTo(raw.priority());
            assertThat(validated.payload()).isEqualTo(raw.payload());
            assertThat(validated.schemaVersion()).isEqualTo("2.0.0");
            assertThat(validated.validatedAt()).isNotNull();
        }

        @Test
        @DisplayName("fromEnriched() deve substituir o payload por dados enriquecidos")
        void fromEnrichedDeveSubstituirPayload() {
            var raw = RawEvent.create("src", "type", EventPriority.NORMAL, Map.of("original", "data"));
            var enrichedPayload = Map.<String, Object>of("original", "data", "extra", "enriched");

            var validated = ValidatedEvent.fromEnriched(raw, enrichedPayload, "2.1.0");

            assertThat(validated.payload()).containsEntry("extra", "enriched");
            assertThat(validated.header()).isEqualTo(raw.header());
        }
    }
}