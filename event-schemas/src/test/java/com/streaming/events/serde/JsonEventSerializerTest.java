package com.streaming.events.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.streaming.events.enums.EventPriority;
import com.streaming.events.model.EventHeader;
import com.streaming.events.model.RawEvent;
import org.apache.kafka.common.errors.SerializationException;
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

@DisplayName("JsonEventSerializer - Testes de Serialização")
class JsonEventSerializerTest {

    private JsonEventSerializer<RawEvent> serializer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        serializer = new JsonEventSerializer<>();
        objectMapper = new ObjectMapper().findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Nested
    @DisplayName("Serialização padrão")
    class SerializacaoPadrao {

        @Test
        @DisplayName("deve serializar RawEvent para JSON bytes")
        void deveSerializarRawEvent() {
            var event = RawEvent.create("ingestion", "CLICK", EventPriority.NORMAL, Map.of("url", "/home"));

            byte[] bytes = serializer.serialize("events.raw.ingest", event);

            assertThat(bytes).isNotNull().isNotEmpty();
            String json = new String(bytes, StandardCharsets.UTF_8);
            assertThat(json).contains("\"url\":\"/home\"");
            assertThat(json).contains("\"priority\":\"NORMAL\"");
        }

        @Test
        @DisplayName("deve serializar Instant como ISO 8601 (não timestamp numérico)")
        void deveSerializarInstantComoIso8601() {
            var event = RawEvent.create("src", "type", EventPriority.LOW, Map.of());

            byte[] bytes = serializer.serialize("topic", event);
            String json = new String(bytes, StandardCharsets.UTF_8);

            // Padrão ISO 8601: ex. "2024-01-15T10:30:00Z" - não deve conter apenas números
            assertThat(json).containsPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
        }

        @Test
        @DisplayName("deve retornar null para dados nulos (Tombstone)")
        void deveRetornarNullParaTombstone() {
            byte[] result = serializer.serialize("topic", null);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Roundtrip Serialização → Desserialização")
    class Roundtrip {

        @Test
        @DisplayName("deve preservar todos os campos no roundtrip RawEvent")
        void devePreservarCamposNoRoundtripRawEvent() throws Exception {
            var header = new EventHeader(
                    UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                    "ingestion-service",
                    "USER_CLICK",
                    Instant.parse("2024-06-15T10:30:00Z"),
                    "corr-abc-123",
                    Map.of("env", "prod")
            );
            var original = new RawEvent(header, EventPriority.HIGH, Map.of("userId", "42"));

            byte[] bytes = serializer.serialize("events.raw.ingest", original);
            RawEvent deserialized = objectMapper.readValue(bytes, RawEvent.class);

            assertThat(deserialized.header().eventId()).isEqualTo(original.header().eventId());
            assertThat(deserialized.header().source()).isEqualTo(original.header().source());
            assertThat(deserialized.header().eventType()).isEqualTo(original.header().eventType());
            assertThat(deserialized.header().timestamp()).isEqualTo(original.header().timestamp());
            assertThat(deserialized.header().correlationId()).isEqualTo(original.header().correlationId());
            assertThat(deserialized.header().metadata()).isEqualTo(original.header().metadata());
            assertThat(deserialized.priority()).isEqualTo(original.priority());
            assertThat(deserialized.payload()).isEqualTo(original.payload());
        }
    }

    @Nested
    @DisplayName("Construtor com ObjectMapper customizado")
    class ConstrutorCustomizado {

        @Test
        @DisplayName("deve usar ObjectMapper injetado")
        void deveUsarObjectMapperInjetado() {
            var customMapper = new ObjectMapper().findAndRegisterModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            var customSerializer = new JsonEventSerializer<RawEvent>(customMapper);

            var event = RawEvent.create("src", "type", EventPriority.LOW, Map.of("k", "v"));
            byte[] bytes = customSerializer.serialize("topic", event);

            assertThat(bytes).isNotNull().isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Erro de serialização")
    class ErroSerializacao {

        @Test
        @DisplayName("deve lançar SerializationException para objeto não-serializável")
        void deveLancarSerializationException() {
            // Um ObjectMapper sem módulos não consegue serializar objetos especiais
            var brokenMapper = new ObjectMapper();
            // Desabilita o auto-discovery para forçar falha com tipos complexos
            var brokenSerializer = new JsonEventSerializer<Object>(brokenMapper);

            // Objeto com referência circular que causa JsonProcessingException
            Object selfRef = new Object() {
                @SuppressWarnings("unused")
                public Object getSelf() { return this; }
            };

            assertThatThrownBy(() -> brokenSerializer.serialize("topic", selfRef))
                    .isInstanceOf(SerializationException.class);
        }
    }
}