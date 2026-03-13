package com.streaming.events.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.streaming.events.enums.EventPriority;
import com.streaming.events.enums.EventStatus;
import com.streaming.events.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de Contrato (Contract Tests) para os schemas de eventos.
 * Garantem que a estrutura JSON produzida pelos modelos respeita
 * o contrato esperado pelos consumidores downstream.
 *
 * Baseado nos princípios do Spring Cloud Contract: verifica a presença
 * e os tipos dos campos obrigatórios no JSON serializado.
 */
@DisplayName("Testes de Contrato - Estrutura JSON dos Eventos")
class EventSchemaContractTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Configuramos exatamente igual ao JsonEventSerializer
        objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // Método utilitário para forçar a serialização real (String) antes de virar nó
    private JsonNode serializeToJson(Object event) throws Exception {
        String jsonString = objectMapper.writeValueAsString(event);
        return objectMapper.readTree(jsonString);
    }

    @Nested
    @DisplayName("Contrato do EventHeader")
    class EventHeaderContract {

        @Test
        @DisplayName("JSON deve conter todos os campos obrigatórios do contrato")
        void deveConterCamposObrigatorios() throws Exception {
            var header = new EventHeader(
                    UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                    "ingestion-service", "USER_CLICK",
                    Instant.parse("2024-06-15T10:30:00Z"),
                    "corr-abc-123", Map.of("env", "prod")
            );

            JsonNode json = serializeToJson(header);

            assertThat(json.has("eventId")).as("Campo 'eventId' obrigatório").isTrue();
            assertThat(json.has("source")).as("Campo 'source' obrigatório").isTrue();
            assertThat(json.has("eventType")).as("Campo 'eventType' obrigatório").isTrue();
            assertThat(json.has("timestamp")).as("Campo 'timestamp' obrigatório").isTrue();
            assertThat(json.has("correlationId")).as("Campo 'correlationId' obrigatório").isTrue();
            assertThat(json.has("metadata")).as("Campo 'metadata' obrigatório").isTrue();

            assertThat(json.get("eventId").asText()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
            assertThat(json.get("source").asText()).isEqualTo("ingestion-service");
            assertThat(json.get("eventType").asText()).isEqualTo("USER_CLICK");
            assertThat(json.get("timestamp").asText()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*");
            assertThat(json.get("metadata").isObject()).isTrue();
        }

        @Test
        @DisplayName("timestamp deve estar no formato ISO 8601")
        void timestampDeveSerIso8601() throws Exception {
            var header = EventHeader.create("src", "type");
            JsonNode json = serializeToJson(header);

            String timestamp = json.get("timestamp").asText();
            // Deve ser parseable por Instant.parse (formato ISO 8601)
            Instant parsed = Instant.parse(timestamp);
            assertThat(parsed).isNotNull();
        }

        @Test
        @DisplayName("eventId deve ser um UUID válido no formato string")
        void eventIdDeveSerUuidValido() throws Exception {
            var header = EventHeader.create("src", "type");
            JsonNode json = serializeToJson(header);

            String eventId = json.get("eventId").asText();
            UUID parsed = UUID.fromString(eventId);
            assertThat(parsed).isNotNull();
        }
    }

    @Nested
    @DisplayName("Contrato do RawEvent")
    class RawEventContract {

        @Test
        @DisplayName("JSON deve conter header, priority e payload")
        void deveConterCamposObrigatorios() throws Exception {
            var event = RawEvent.create("ingestion", "CLICK", EventPriority.NORMAL, Map.of("url", "/home"));

            JsonNode json = serializeToJson(event);

            assertThat(json.has("header")).as("Campo 'header' obrigatório").isTrue();
            assertThat(json.has("priority")).as("Campo 'priority' obrigatório").isTrue();
            assertThat(json.has("payload")).as("Campo 'payload' obrigatório").isTrue();

            assertThat(json.get("header").isObject()).isTrue();
            assertThat(json.get("priority").asText()).isEqualTo("NORMAL");
            assertThat(json.get("payload").isObject()).isTrue();
        }

        @Test
        @DisplayName("priority deve ser um dos valores válidos do enum")
        void priorityDeveSerValorValidoDoEnum() throws Exception {
            for (EventPriority priority : EventPriority.values()) {
                var event = RawEvent.create("src", "type", priority, Map.of());
                JsonNode json = serializeToJson(event);
                assertThat(json.get("priority").asText()).isEqualTo(priority.name());
            }
        }
    }

    @Nested
    @DisplayName("Contrato do ValidatedEvent")
    class ValidatedEventContract {

        @Test
        @DisplayName("JSON deve conter header, priority, payload, validatedAt e schemaVersion")
        void deveConterCamposObrigatorios() throws Exception {
            var raw = RawEvent.create("src", "type", EventPriority.HIGH, Map.of("d", "v"));
            var event = ValidatedEvent.from(raw, "2.0.0");

            JsonNode json = serializeToJson(event);

            assertThat(json.has("header")).isTrue();
            assertThat(json.has("priority")).isTrue();
            assertThat(json.has("payload")).isTrue();
            assertThat(json.has("validatedAt")).as("Campo 'validatedAt' obrigatório").isTrue();
            assertThat(json.has("schemaVersion")).as("Campo 'schemaVersion' obrigatório").isTrue();

            assertThat(json.get("schemaVersion").asText()).isEqualTo("2.0.0");
            // validatedAt deve ser ISO 8601
            Instant.parse(json.get("validatedAt").asText());
        }
    }

    @Nested
    @DisplayName("Contrato do ProcessedEvent")
    class ProcessedEventContract {

        @Test
        @DisplayName("JSON deve conter header, priority, status, payload, result, processedAt e payloadHash")
        void deveConterCamposObrigatorios() throws Exception {
            var header = EventHeader.create("src", "type");
            var event = new ProcessedEvent(
                    header, EventPriority.NORMAL, EventStatus.PROCESSED,
                    Map.of("d", "v"), Map.of("count", 10),
                    Instant.parse("2024-06-15T10:30:00Z"), "sha256-abc"
            );

            JsonNode json = serializeToJson(event);

            assertThat(json.has("header")).isTrue();
            assertThat(json.has("priority")).isTrue();
            assertThat(json.has("status")).as("Campo 'status' obrigatório").isTrue();
            assertThat(json.has("payload")).isTrue();
            assertThat(json.has("result")).as("Campo 'result' obrigatório").isTrue();
            assertThat(json.has("processedAt")).as("Campo 'processedAt' obrigatório").isTrue();
            assertThat(json.has("payloadHash")).as("Campo 'payloadHash' obrigatório").isTrue();

            assertThat(json.get("status").asText()).isEqualTo("PROCESSED");
            assertThat(json.get("payloadHash").asText()).isEqualTo("sha256-abc");
        }

        @Test
        @DisplayName("status deve ser um dos valores válidos do enum EventStatus")
        void statusDeveSerValorValidoDoEnum() throws Exception {
            var header = EventHeader.create("src", "type");

            for (EventStatus status : EventStatus.values()) {
                var event = new ProcessedEvent(
                        header, EventPriority.NORMAL, status,
                        Map.of("d", "v"), Map.of(), Instant.now(), "hash"
                );
                JsonNode json = serializeToJson(event);
                assertThat(json.get("status").asText()).isEqualTo(status.name());
            }
        }
    }

    @Nested
    @DisplayName("Contrato do AlertEvent")
    class AlertEventContract {

        @Test
        @DisplayName("JSON deve conter header, priority, ruleName, description, sourceEvent e triggeredAt")
        void deveConterCamposObrigatorios() throws Exception {
            var header = EventHeader.create("src", "ALERT");
            var event = new AlertEvent(
                    header, EventPriority.CRITICAL, "highCpu",
                    "CPU acima de 90%", Map.of("hostId", "srv-01"),
                    Instant.parse("2024-06-15T10:30:00Z")
            );

            JsonNode json = serializeToJson(event);

            assertThat(json.has("header")).isTrue();
            assertThat(json.has("priority")).isTrue();
            assertThat(json.has("ruleName")).as("Campo 'ruleName' obrigatório").isTrue();
            assertThat(json.has("description")).as("Campo 'description' obrigatório").isTrue();
            assertThat(json.has("sourceEvent")).as("Campo 'sourceEvent' obrigatório").isTrue();
            assertThat(json.has("triggeredAt")).as("Campo 'triggeredAt' obrigatório").isTrue();

            assertThat(json.get("ruleName").asText()).isEqualTo("highCpu");
            assertThat(json.get("description").asText()).isEqualTo("CPU acima de 90%");
            assertThat(json.get("sourceEvent").isObject()).isTrue();
        }
    }

    @Nested
    @DisplayName("Contrato do DlqEvent")
    class DlqEventContract {

        @Test
        @DisplayName("JSON deve conter dlqId, originalEvent, originTopic, errorMessage, stackTrace, retryCount e failedAt")
        void deveConterCamposObrigatorios() throws Exception {
            var event = new DlqEvent(
                    UUID.fromString("660e8400-e29b-41d4-a716-446655440000"),
                    Map.of("data", "val"), "events.raw.ingest",
                    "Erro de validação", "java.lang.Exception at...",
                    3, Instant.parse("2024-06-15T10:30:00Z")
            );

            JsonNode json = serializeToJson(event);

            assertThat(json.has("dlqId")).as("Campo 'dlqId' obrigatório").isTrue();
            assertThat(json.has("originalEvent")).as("Campo 'originalEvent' obrigatório").isTrue();
            assertThat(json.has("originTopic")).as("Campo 'originTopic' obrigatório").isTrue();
            assertThat(json.has("errorMessage")).as("Campo 'errorMessage' obrigatório").isTrue();
            assertThat(json.has("stackTrace")).as("Campo 'stackTrace' obrigatório").isTrue();
            assertThat(json.has("retryCount")).as("Campo 'retryCount' obrigatório").isTrue();
            assertThat(json.has("failedAt")).as("Campo 'failedAt' obrigatório").isTrue();

            assertThat(json.get("dlqId").asText()).isEqualTo("660e8400-e29b-41d4-a716-446655440000");
            assertThat(json.get("retryCount").asInt()).isEqualTo(3);
            assertThat(json.get("originTopic").asText()).isEqualTo("events.raw.ingest");
        }

        @Test
        @DisplayName("retryCount deve ser um inteiro não negativo")
        void retryCountDeveSerInteiroNaoNegativo() throws Exception {
            var event = DlqEvent.create(Map.of("d", "v"), "topic", "msg", "stack", 0);
            JsonNode json = serializeToJson(event);

            assertThat(json.get("retryCount").isInt()).isTrue();
            assertThat(json.get("retryCount").asInt()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Contrato cross-cutting: Propagação do correlationId")
    class PropagacaoCorrelationId {

        @Test
        @DisplayName("correlationId deve ser propagado ao longo de todo o ciclo de vida do evento")
        void correlationIdDeveSerPropagadoNoCicloDeVida() throws Exception {
            // 1. RawEvent (início da cadeia)
            var raw = RawEvent.create("ingestion", "CLICK", EventPriority.HIGH, Map.of("url", "/home"));
            String correlationId = raw.header().correlationId();

            // 2. ValidatedEvent (mantém o mesmo header e correlationId)
            var validated = ValidatedEvent.from(raw, "1.0");
            assertThat(validated.header().correlationId()).isEqualTo(correlationId);

            // 3. ProcessedEvent (mantém o mesmo header e correlationId)
            var processed = ProcessedEvent.from(validated, Map.of("count", 1), "hash");
            assertThat(processed.header().correlationId()).isEqualTo(correlationId);

            // 4. AlertEvent (propaga correlationId via factory method)
            var alert = AlertEvent.create(raw.header(), EventPriority.CRITICAL, "rule", "desc", Map.of());
            assertThat(alert.header().correlationId()).isEqualTo(correlationId);

            // Verifica nos JSONs serializados
            JsonNode rawJson = serializeToJson(raw);
            JsonNode validatedJson = serializeToJson(validated);
            JsonNode processedJson = serializeToJson(processed);
            JsonNode alertJson = serializeToJson(alert);

            assertThat(rawJson.get("header").get("correlationId").asText()).isEqualTo(correlationId);
            assertThat(validatedJson.get("header").get("correlationId").asText()).isEqualTo(correlationId);
            assertThat(processedJson.get("header").get("correlationId").asText()).isEqualTo(correlationId);
            assertThat(alertJson.get("header").get("correlationId").asText()).isEqualTo(correlationId);
        }
    }
}