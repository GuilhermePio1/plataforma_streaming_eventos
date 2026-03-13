package com.streaming.events.model;

import com.streaming.events.enums.EventPriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RawEvent - Testes de Integridade")
class RawEventTest {

    @Nested
    @DisplayName("Construtor compacto (validações)")
    class ConstrutorCompacto {

        @Test
        @DisplayName("deve criar RawEvent com campos válidos")
        void deveCriarRawEventValido() {
            var header = EventHeader.create("source", "type");
            var payload = Map.<String, Object>of("key", "value");

            var event = new RawEvent(header, EventPriority.NORMAL, payload);

            assertThat(event.header()).isEqualTo(header);
            assertThat(event.priority()).isEqualTo(EventPriority.NORMAL);
            assertThat(event.payload()).containsEntry("key", "value");
        }

        @Test
        @DisplayName("deve rejeitar header nulo")
        void deveRejeitarHeaderNulo() {
            assertThatThrownBy(() -> new RawEvent(null, EventPriority.NORMAL, Map.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("header");
        }

        @Test
        @DisplayName("deve rejeitar priority nula")
        void deveRejeitarPriorityNula() {
            assertThatThrownBy(() -> new RawEvent(EventHeader.create("src", "type"), null, Map.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("priority");
        }
    }

    @Nested
    @DisplayName("Imutabilidade do payload")
    class Imutabilidade {

        @Test
        @DisplayName("payload deve ser imutável após criação")
        void payloadDeveSerImutavel() {
            var event = RawEvent.create("src", "type", EventPriority.LOW, Map.of("k", "v"));

            assertThatThrownBy(() -> event.payload().put("nova", "entrada"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("mutação no mapa original não deve afetar o evento")
        void mutacaoExternaNaoAfetaEvento() {
            var mutablePayload = new HashMap<String, Object>();
            mutablePayload.put("key", "original");

            var event = new RawEvent(EventHeader.create("src", "type"), EventPriority.NORMAL, mutablePayload);

            mutablePayload.put("key", "modificado");

            assertThat(event.payload()).containsEntry("key", "original");
        }

        @Test
        @DisplayName("payload nulo deve resultar em mapa vazio")
        void payloadNuloDeveSerMapaVazio() {
            var event = new RawEvent(EventHeader.create("src", "type"), EventPriority.NORMAL, null);

            assertThat(event.payload()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Factory method create")
    class FactoryMethod {

        @Test
        @DisplayName("deve criar evento com header gerado automaticamente")
        void deveCriarEventoComHeaderAutomatico() {
            var event = RawEvent.create("ingestion-service", "CLICK", EventPriority.HIGH, Map.of("url", "/home"));

            assertThat(event.header()).isNotNull();
            assertThat(event.header().source()).isEqualTo("ingestion-service");
            assertThat(event.header().eventType()).isEqualTo("CLICK");
            assertThat(event.priority()).isEqualTo(EventPriority.HIGH);
            assertThat(event.payload()).containsEntry("url", "/home");
        }
    }

    @Nested
    @DisplayName("Contrato de equals/hashCode (Java Record)")
    class EqualsHashCode {

        @Test
        @DisplayName("dois eventos com mesmo header, priority e payload devem ser iguais")
        void eventosComMesmosCamposDevemSerIguais() {
            var header = EventHeader.create("src", "type");
            var payload = Map.<String, Object>of("k", "v");

            var e1 = new RawEvent(header, EventPriority.NORMAL, payload);
            var e2 = new RawEvent(header, EventPriority.NORMAL, payload);

            assertThat(e1).isEqualTo(e2);
            assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        }
    }
}
