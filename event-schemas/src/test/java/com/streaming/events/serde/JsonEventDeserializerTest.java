package com.streaming.events.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.streaming.events.enums.EventPriority;
import com.streaming.events.exception.EventDeserializationException;
import com.streaming.events.model.EventHeader;
import com.streaming.events.model.RawEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JsonEventDeserializer - Testes de Desserialização")
class JsonEventDeserializerTest {

    private JsonEventDeserializer<RawEvent> deserializer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        deserializer = new JsonEventDeserializer<>(RawEvent.class);
        objectMapper = new ObjectMapper().findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Nested
    @DisplayName("Desserialização padrão")
    class DeserializacaoPadrao {

        @Test
        @DisplayName("deve desserializar JSON bytes em RawEvent")
        void deveDesserializarRawEvent() throws Exception {
            var header = new EventHeader(
                    UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                    "ingestion-service",
                    "USER_CLICK",
                    Instant.parse("2024-06-15T10:30:00Z"),
                    "corr-abc-123",
                    Map.of("env", "prod")
            );
            var original = new RawEvent(header, EventPriority.HIGH, Map.of("userId", "42"));
            byte[] bytes = objectMapper.writeValueAsBytes(original);

            RawEvent deserialized = deserializer.deserialize("events.raw.ingest", bytes);

            assertThat(deserialized).isNotNull();
            assertThat(deserialized.header().eventId()).isEqualTo(original.header().eventId());
            assertThat(deserialized.header().source()).isEqualTo(original.header().source());
            assertThat(deserialized.header().timestamp()).isEqualTo(original.header().timestamp());
            assertThat(deserialized.priority()).isEqualTo(EventPriority.HIGH);
            assertThat(deserialized.payload()).containsEntry("userId", "42");
        }

        @Test
        @DisplayName("deve retornar null para dados nulos (Tombstone)")
        void deveRetornarNullParaDadosNulos() {
            assertThat(deserializer.deserialize("topic", null)).isNull();
        }

        @Test
        @DisplayName("deve retornar null para dados vazios (Tombstone)")
        void deveRetornarNullParaDadosVazios() {
            assertThat(deserializer.deserialize("topic", new byte[0])).isNull();
        }
    }

    @Nested
    @DisplayName("Tolerância a evolução de schema")
    class EvolucaoSchema {

        @Test
        @DisplayName("deve ignorar campos desconhecidos no JSON (forward compatibility)")
        void deveIgnorarCamposDesconhecidos() {
            String jsonComCampoExtra = """
                    {
                        "header": {
                            "eventId": "550e8400-e29b-41d4-a716-446655440000",
                            "source": "src",
                            "eventType": "type",
                            "timestamp": "2024-06-15T10:30:00Z",
                            "correlationId": "corr-123",
                            "metadata": {}
                        },
                        "priority": "NORMAL",
                        "payload": {"key": "value"},
                        "campoFuturoDesconhecido": "deve ser ignorado"
                    }
                    """;

            byte[] bytes = jsonComCampoExtra.getBytes(StandardCharsets.UTF_8);
            RawEvent event = deserializer.deserialize("topic", bytes);

            assertThat(event).isNotNull();
            assertThat(event.header().source()).isEqualTo("src");
            assertThat(event.payload()).containsEntry("key", "value");
        }
    }

    @Nested
    @DisplayName("Poison Pill (JSON inválido)")
    class PoisonPill {

        @Test
        @DisplayName("deve lançar EventDeserializationException para JSON malformado")
        void deveLancarExceptionParaJsonMalformado() {
            byte[] invalidJson = "{ json quebrado !!!".getBytes(StandardCharsets.UTF_8);

            assertThatThrownBy(() -> deserializer.deserialize("events.raw.ingest", invalidJson))
                    .isInstanceOf(EventDeserializationException.class)
                    .satisfies(ex -> {
                        var deserEx = (EventDeserializationException) ex;
                        assertThat(deserEx.getTopic()).isEqualTo("events.raw.ingest");
                        assertThat(deserEx.getRawPayload()).isEqualTo("{ json quebrado !!!");
                    });
        }

        @Test
        @DisplayName("deve preservar raw payload na exceção para análise na DLQ")
        void devePreservarRawPayloadNaExcecao() {
            String invalidPayload = "{\"header\": \"invalido\"}";
            byte[] bytes = invalidPayload.getBytes(StandardCharsets.UTF_8);

            assertThatThrownBy(() -> deserializer.deserialize("events.validated", bytes))
                    .isInstanceOf(EventDeserializationException.class)
                    .satisfies(ex -> {
                        var deserEx = (EventDeserializationException) ex;
                        assertThat(deserEx.getRawPayload()).isEqualTo(invalidPayload);
                        assertThat(deserEx.getTopic()).isEqualTo("events.validated");
                    });
        }

        @Test
        @DisplayName("deve incluir causa original (IOException) na exceção")
        void deveIncluirCausaOriginal() {
            byte[] invalidJson = "não é json".getBytes(StandardCharsets.UTF_8);

            assertThatThrownBy(() -> deserializer.deserialize("topic", invalidJson))
                    .isInstanceOf(EventDeserializationException.class)
                    .hasCauseInstanceOf(java.io.IOException.class);
        }
    }

    @Nested
    @DisplayName("Roundtrip completo Serialização → Desserialização")
    class Roundtrip {

        @Test
        @DisplayName("deve preservar todos os campos no roundtrip com Serializer + Deserializer")
        void devePreservarCamposNoRoundtrip() {
            var serializer = new JsonEventSerializer<RawEvent>();
            var header = new EventHeader(
                    UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                    "ingestion-service",
                    "USER_CLICK",
                    Instant.parse("2024-06-15T10:30:00Z"),
                    "corr-abc-123",
                    Map.of("env", "prod")
            );
            var original = new RawEvent(header, EventPriority.CRITICAL, Map.of("userId", "42", "action", "click"));

            byte[] bytes = serializer.serialize("events.raw.ingest", original);
            RawEvent deserialized = deserializer.deserialize("events.raw.ingest", bytes);

            assertThat(deserialized).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("Construtor com ObjectMapper customizado")
    class ConstrutorCustomizado {

        @Test
        @DisplayName("deve usar ObjectMapper injetado para desserialização")
        void deveUsarObjectMapperInjetado() throws Exception {
            var customMapper = new ObjectMapper().findAndRegisterModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            var customDeserializer = new JsonEventDeserializer<>(RawEvent.class, customMapper);

            var original = RawEvent.create("src", "type", EventPriority.LOW, Map.of("k", "v"));
            byte[] bytes = customMapper.writeValueAsBytes(original);

            RawEvent deserialized = customDeserializer.deserialize("topic", bytes);

            assertThat(deserialized).isNotNull();
            assertThat(deserialized.priority()).isEqualTo(EventPriority.LOW);
        }
    }
}