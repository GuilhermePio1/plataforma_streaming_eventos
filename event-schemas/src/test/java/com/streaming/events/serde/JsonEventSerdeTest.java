package com.streaming.events.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.streaming.events.enums.EventPriority;
import com.streaming.events.model.*;
import com.streaming.events.enums.EventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JsonEventSerde - Testes de Serialização/Desserialização combinados")
class JsonEventSerdeTest {

    @Nested
    @DisplayName("Roundtrip para cada tipo de evento")
    class RoundtripPorTipoDeEvento {

        @Test
        @DisplayName("RawEvent: serialize → deserialize deve preservar todos os campos")
        void roundtripRawEvent() {
            var serde = new JsonEventSerde<>(RawEvent.class);
            var original = RawEvent.create("ingestion", "CLICK", EventPriority.HIGH, Map.of("url", "/home", "userId", "42"));

            byte[] bytes = serde.serializer().serialize("events.raw.ingest", original);
            RawEvent deserialized = serde.deserializer().deserialize("events.raw.ingest", bytes);

            assertThat(deserialized).isEqualTo(original);
        }

        @Test
        @DisplayName("ValidatedEvent: serialize → deserialize deve preservar todos os campos")
        void roundtripValidatedEvent() {
            var serde = new JsonEventSerde<>(ValidatedEvent.class);
            var raw = RawEvent.create("src", "type", EventPriority.NORMAL, Map.of("data", "val"));
            var header = raw.header();
            var original = new ValidatedEvent(header, EventPriority.NORMAL, Map.of("data", "val"), Instant.parse("2024-06-15T10:30:00Z"), "1.2.0");

            byte[] bytes = serde.serializer().serialize("events.validated", original);
            ValidatedEvent deserialized = serde.deserializer().deserialize("events.validated", bytes);

            assertThat(deserialized).isEqualTo(original);
        }

        @Test
        @DisplayName("ProcessedEvent: serialize → deserialize deve preservar todos os campos")
        void roundtripProcessedEvent() {
            var serde = new JsonEventSerde<>(ProcessedEvent.class);
            var header = new EventHeader(
                    UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                    "stream-processing", "PROCESSED",
                    Instant.parse("2024-06-15T10:30:00Z"), "corr-123", Map.of()
            );
            var original = new ProcessedEvent(
                    header, EventPriority.HIGH, EventStatus.PROCESSED,
                    Map.of("data", "val"), Map.of("count", 10),
                    Instant.parse("2024-06-15T10:31:00Z"), "sha256-abc123"
            );

            byte[] bytes = serde.serializer().serialize("events.processed", original);
            ProcessedEvent deserialized = serde.deserializer().deserialize("events.processed", bytes);

            assertThat(deserialized).isEqualTo(original);
        }

        @Test
        @DisplayName("AlertEvent: serialize → deserialize deve preservar todos os campos")
        void roundtripAlertEvent() {
            var serde = new JsonEventSerde<>(AlertEvent.class);
            var header = new EventHeader(
                    UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                    "stream-processing-service", "ALERT",
                    Instant.parse("2024-06-15T10:30:00Z"), "corr-123", Map.of("causedByEventId", "abc")
            );
            var original = new AlertEvent(
                    header, EventPriority.CRITICAL, "highCpu",
                    "CPU acima de 90%", Map.of("hostId", "srv-01"),
                    Instant.parse("2024-06-15T10:30:05Z")
            );

            byte[] bytes = serde.serializer().serialize("events.alerts", original);
            AlertEvent deserialized = serde.deserializer().deserialize("events.alerts", bytes);

            assertThat(deserialized).isEqualTo(original);
        }

        @Test
        @DisplayName("DlqEvent: serialize → deserialize deve preservar todos os campos")
        void roundtripDlqEvent() {
            var serde = new JsonEventSerde<>(DlqEvent.class);
            var original = new DlqEvent(
                    UUID.fromString("660e8400-e29b-41d4-a716-446655440000"),
                    Map.of("event", "data"), "events.raw.ingest",
                    "Falha de validação", "java.lang.Exception at...",
                    3, Instant.parse("2024-06-15T10:32:00Z")
            );

            byte[] bytes = serde.serializer().serialize("events.dlq", original);
            DlqEvent deserialized = serde.deserializer().deserialize("events.dlq", bytes);

            assertThat(deserialized).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("Tombstone (null data)")
    class Tombstone {

        @Test
        @DisplayName("serializer deve retornar null para data null")
        void serializerDeveRetornarNullParaTombstone() {
            var serde = new JsonEventSerde<>(RawEvent.class);
            assertThat(serde.serializer().serialize("topic", null)).isNull();
        }

        @Test
        @DisplayName("deserializer deve retornar null para data null")
        void deserializerDeveRetornarNullParaTombstone() {
            var serde = new JsonEventSerde<>(RawEvent.class);
            assertThat(serde.deserializer().deserialize("topic", null)).isNull();
        }
    }

    @Nested
    @DisplayName("Ciclo de vida do Serde")
    class CicloDeVida {

        @Test
        @DisplayName("configure e close devem executar sem erros")
        void configureECloseDevemFuncionar() {
            var serde = new JsonEventSerde<>(RawEvent.class);

            serde.configure(Map.of(), false);
            serde.close();
        }

        @Test
        @DisplayName("serializer() e deserializer() devem retornar instâncias válidas")
        void deveRetornarInstanciasValidas() {
            var serde = new JsonEventSerde<>(RawEvent.class);

            assertThat(serde.serializer()).isNotNull().isInstanceOf(JsonEventSerializer.class);
            assertThat(serde.deserializer()).isNotNull().isInstanceOf(JsonEventDeserializer.class);
        }
    }

    @Nested
    @DisplayName("Construtor com ObjectMapper customizado")
    class ConstrutorCustomizado {

        @Test
        @DisplayName("deve funcionar com ObjectMapper injetado")
        void deveUsarObjectMapperInjetado() {
            var customMapper = new ObjectMapper().findAndRegisterModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            var serde = new JsonEventSerde<>(RawEvent.class, customMapper);

            var original = RawEvent.create("src", "type", EventPriority.LOW, Map.of("k", "v"));

            byte[] bytes = serde.serializer().serialize("topic", original);
            RawEvent deserialized = serde.deserializer().deserialize("topic", bytes);

            assertThat(deserialized).isEqualTo(original);
        }
    }
}