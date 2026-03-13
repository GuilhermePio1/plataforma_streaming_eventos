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

@DisplayName("AlertEvent - Testes de Integridade")
class AlertEventTest {

    private final EventHeader header = EventHeader.create("src", "type");

    @Nested
    @DisplayName("Construtor compacto (validações)")
    class ConstrutorCompacto {

        @Test
        @DisplayName("deve criar AlertEvent com campos válidos")
        void deveCriarAlertEventValido() {
            var sourceEvent = Map.<String, Object>of("key", "value");
            var now = Instant.now();

            var event = new AlertEvent(header, EventPriority.CRITICAL, "highCpu", "CPU acima de 90%", sourceEvent, now);

            assertThat(event.header()).isEqualTo(header);
            assertThat(event.priority()).isEqualTo(EventPriority.CRITICAL);
            assertThat(event.ruleName()).isEqualTo("highCpu");
            assertThat(event.description()).isEqualTo("CPU acima de 90%");
            assertThat(event.sourceEvent()).containsEntry("key", "value");
            assertThat(event.triggeredAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("deve rejeitar header nulo")
        void deveRejeitarHeaderNulo() {
            assertThatThrownBy(() -> new AlertEvent(null, EventPriority.HIGH, "rule", "desc", Map.of(), Instant.now()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("deve rejeitar ruleName nulo")
        void deveRejeitarRuleNameNulo() {
            assertThatThrownBy(() -> new AlertEvent(header, EventPriority.HIGH, null, "desc", Map.of(), Instant.now()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("O nome da regra (ruleName) não pode ser nulo");
        }

        @Test
        @DisplayName("deve rejeitar ruleName vazio")
        void deveRejeitarRuleNameVazio() {
            assertThatThrownBy(() -> new AlertEvent(header, EventPriority.HIGH, "", "desc", Map.of(), Instant.now()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("deve rejeitar description nula")
        void deveRejeitarDescriptionNula() {
            assertThatThrownBy(() -> new AlertEvent(header, EventPriority.HIGH, "rule", null, Map.of(), Instant.now()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("deve rejeitar description vazia")
        void deveRejeitarDescriptionVazia() {
            assertThatThrownBy(() -> new AlertEvent(header, EventPriority.HIGH, "rule", "", Map.of(), Instant.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("A descrição não pode ser vazia");
        }

        @Test
        @DisplayName("deve rejeitar triggeredAt nulo")
        void deveRejeitarTriggeredAtNulo() {
            assertThatThrownBy(() -> new AlertEvent(header, EventPriority.HIGH, "rule", "desc", Map.of(), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Imutabilidade do sourceEvent")
    class Imutabilidade {

        @Test
        @DisplayName("sourceEvent deve ser imutável após criação")
        void sourceEventDeveSerImutavel() {
            var event = new AlertEvent(header, EventPriority.HIGH, "rule", "desc", Map.of("k", "v"), Instant.now());

            assertThatThrownBy(() -> event.sourceEvent().put("nova", "entrada"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("mutação no mapa original não deve afetar o evento")
        void mutacaoExternaNaoAfetaEvento() {
            var mutableMap = new HashMap<String, Object>();
            mutableMap.put("key", "original");

            var event = new AlertEvent(header, EventPriority.HIGH, "rule", "desc", mutableMap, Instant.now());

            mutableMap.put("key", "modificado");
            assertThat(event.sourceEvent()).containsEntry("key", "original");
        }
    }

    @Nested
    @DisplayName("Factory method create()")
    class FactoryMethod {

        @Test
        @DisplayName("deve propagar correlationId do header original")
        void devePropagarCorrelationId() {
            var originalHeader = EventHeader.create("ingestion", "CLICK");
            var sourceEvent = Map.<String, Object>of("userId", "123");

            var alert = AlertEvent.create(originalHeader, EventPriority.CRITICAL, "highTraffic", "Tráfego elevado", sourceEvent);

            assertThat(alert.header().correlationId()).isEqualTo(originalHeader.correlationId());
            assertThat(alert.header().source()).isEqualTo("stream-processing-service");
            assertThat(alert.header().eventType()).isEqualTo("ALERT");
            assertThat(alert.header().metadata()).containsEntry("causedByEventId", originalHeader.eventId().toString());
            assertThat(alert.ruleName()).isEqualTo("highTraffic");
            assertThat(alert.triggeredAt()).isNotNull();
        }
    }
}