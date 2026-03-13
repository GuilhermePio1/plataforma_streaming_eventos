package com.streaming.events.model;

import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EventHeader - Testes de Integridade")
class EventHeaderTest {

    @Nested
    @DisplayName("Construtor compacto (validações)")
    class ConstrutorCompacto {

        @Test
        @DisplayName("deve criar header com todos os campos válidos")
        void deveCriarHeaderValido() {
            var id = UUID.randomUUID();
            var now = Instant.now();
            var metadata = Map.of("key", "value");

            var header = new EventHeader(id, "source", "type", now, "corr-123", metadata);

            assertThat(header.eventId()).isEqualTo(id);
            assertThat(header.source()).isEqualTo("source");
            assertThat(header.eventType()).isEqualTo("type");
            assertThat(header.timestamp()).isEqualTo(now);
            assertThat(header.correlationId()).isEqualTo("corr-123");
            assertThat(header.metadata()).containsEntry("key", "value");
        }

        @Test
        @DisplayName("deve rejeitar eventId nulo")
        void deveRejeitarEventIdNulo() {
            assertThatThrownBy(() -> new EventHeader(null, "source", "type", Instant.now(), null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("eventId");
        }

        @Test
        @DisplayName("deve rejeitar source nulo")
        void deveRejeitarSourceNulo() {
            assertThatThrownBy(() -> new EventHeader(UUID.randomUUID(), null, "type", Instant.now(), null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("source");
        }

        @Test
        @DisplayName("deve rejeitar eventType nulo")
        void deveRejeitarEventTypeNulo() {
            assertThatThrownBy(() -> new EventHeader(UUID.randomUUID(), "source", null, Instant.now(), null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("eventType");
        }

        @Test
        @DisplayName("deve rejeitar timestamp nulo")
        void deveRejeitarTimestampNulo() {
            assertThatThrownBy(() -> new EventHeader(UUID.randomUUID(), "source", "type", null, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("timestamp");
        }

        @Test
        @DisplayName("deve rejeitar source vazio")
        void deveRejeitarSourceVazio() {
            assertThatThrownBy(() -> new EventHeader(UUID.randomUUID(), "", "type", Instant.now(), null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("source");
        }

        @Test
        @DisplayName("deve rejeitar source com apenas espaços em branco")
        void deveRejeitarSourceEmBranco() {
            assertThatThrownBy(() -> new EventHeader(UUID.randomUUID(), "   ", "type", Instant.now(), null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("source");
        }

        @Test
        @DisplayName("deve rejeitar eventType vazio")
        void deveRejeitarEventTypeVazio() {
            assertThatThrownBy(() -> new EventHeader(UUID.randomUUID(), "source", "", Instant.now(), null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("eventType");
        }
    }

    @Nested
    @DisplayName("Imutabilidade profunda")
    class Imutabilidade {

        @Test
        @DisplayName("metadata deve ser imutável após criação")
        void metadataDeveSerImutavel() {
            var mutableMap = new HashMap<String, String>();
            mutableMap.put("key", "value");

            var header = new EventHeader(UUID.randomUUID(), "source", "type", Instant.now(), null, mutableMap);

            assertThatThrownBy(() -> header.metadata().put("nova", "entrada"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("mutação no mapa original não deve afetar o header")
        void mutacaoExternaNaoAfetaHeader() {
            var mutableMap = new HashMap<String, String>();
            mutableMap.put("key", "original");

            var header = new EventHeader(UUID.randomUUID(), "source", "type", Instant.now(), null, mutableMap);

            mutableMap.put("key", "modificado");

            assertThat(header.metadata()).containsEntry("key", "original");
        }

        @Test
        @DisplayName("metadata nulo deve resultar em mapa vazio imutável")
        void metadataNuloDeveSerMapaVazio() {
            var header = new EventHeader(UUID.randomUUID(), "source", "type", Instant.now(), null, null);

            assertThat(header.metadata()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("create(source, eventType) deve gerar eventId, timestamp e correlationId automaticamente")
        void createSimpleDeveGerarCamposAutomaticamente() {
            var header = EventHeader.create("ingestion-service", "USER_CLICK");

            assertThat(header.eventId()).isNotNull();
            assertThat(header.source()).isEqualTo("ingestion-service");
            assertThat(header.eventType()).isEqualTo("USER_CLICK");

            // MELHORIA AQUI: Garante que o timestamp foi gerado "agora", com tolerância de 1 segundo
            assertThat(header.timestamp())
                    .isNotNull()
                    .isCloseTo(Instant.now(), new TemporalUnitWithinOffset(1, ChronoUnit.SECONDS));

            assertThat(header.correlationId()).isNotNull().isNotBlank();
            assertThat(header.metadata()).isEmpty();
        }

        @Test
        @DisplayName("create com correlationId deve propagar o ID existente")
        void createComCorrelationIdDevePropagarId() {
            var correlationId = "trace-abc-123";
            var metadata = Map.of("origin", "test");

            var header = EventHeader.create("service-a", "EVENT_X", correlationId, metadata);

            assertThat(header.correlationId()).isEqualTo(correlationId);
            assertThat(header.metadata()).containsEntry("origin", "test");
        }
    }

    @Nested
    @DisplayName("Contrato de equals/hashCode (Java Record)")
    class EqualsHashCode {

        @Test
        @DisplayName("dois headers com mesmos campos devem ser iguais")
        void headersComMesmosCamposDevemSerIguais() {
            var id = UUID.randomUUID();
            var now = Instant.now();

            var h1 = new EventHeader(id, "src", "type", now, "corr", Map.of());
            var h2 = new EventHeader(id, "src", "type", now, "corr", Map.of());

            assertThat(h1).isEqualTo(h2);
            assertThat(h1.hashCode()).isEqualTo(h2.hashCode());
        }

        @Test
        @DisplayName("headers com campos diferentes não devem ser iguais")
        void headersComCamposDiferentesNaoDevemSerIguais() {
            var now = Instant.now();

            var h1 = new EventHeader(UUID.randomUUID(), "src", "type", now, null, null);
            var h2 = new EventHeader(UUID.randomUUID(), "src", "type", now, null, null);

            assertThat(h1).isNotEqualTo(h2);
        }
    }
}