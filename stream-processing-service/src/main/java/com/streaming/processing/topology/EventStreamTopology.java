package com.streaming.processing.topology;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streaming.events.enums.EventPriority;
import com.streaming.events.model.*;
import com.streaming.events.serde.JsonEventSerde;
import com.streaming.events.topic.KafkaTopics;
import com.streaming.processing.service.EventValidationService;
import com.streaming.processing.service.PayloadHashService;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Topologia principal do Kafka Streams.
 * Define o pipeline de processamento: Raw → Validated → Processed → Alerts/DLQ.
 */
@Component
public class EventStreamTopology {

    private static final Logger log = LoggerFactory.getLogger(EventStreamTopology.class);

    private final EventValidationService validationService;
    private final PayloadHashService hashService;
    private final ObjectMapper objectMapper;

    public EventStreamTopology(EventValidationService validationService,
                               PayloadHashService hashService) {
        this.validationService = validationService;
        this.hashService = hashService;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Autowired
    public void buildTopology(StreamsBuilder builder) {
        JsonEventSerde<RawEvent> rawEventSerde = new JsonEventSerde<>(RawEvent.class);
        JsonEventSerde<ValidatedEvent> validatedEventSerde = new JsonEventSerde<>(ValidatedEvent.class);
        JsonEventSerde<ProcessedEvent> processedEventSerde = new JsonEventSerde<>(ProcessedEvent.class);
        JsonEventSerde<AlertEvent> alertEventSerde = new JsonEventSerde<>(AlertEvent.class);
        JsonEventSerde<DlqEvent> dlqEventSerde = new JsonEventSerde<>(DlqEvent.class);

        // 1. Consome eventos brutos do tópico de ingestão
        KStream<String, RawEvent> rawStream = builder.stream(
                KafkaTopics.RAW_INGEST,
                Consumed.with(Serdes.String(), rawEventSerde)
                        .withName("source-raw-events")
        );

        // 2. Validação: separa eventos válidos de inválidos usando split() (API moderna)
        Map<String, KStream<String, RawEvent>> branches = rawStream.split(Named.as("validation-"))
                .branch((key, raw) -> validationService.validate(raw) != null,
                        Branched.as("valid"))
                .defaultBranch(Branched.as("invalid"));

        KStream<String, RawEvent> validStream = branches.get("validation-valid");
        KStream<String, RawEvent> invalidStream = branches.get("validation-invalid");

        // 3. Converte eventos válidos em ValidatedEvent
        KStream<String, ValidatedEvent> validatedStream = validStream
                .mapValues((key, raw) -> validationService.validateAndEnrich(raw),
                        Named.as("enrich-events"))
                .peek((key, validated) ->
                        log.info("Evento validado: eventId={}", validated.header().eventId()),
                        Named.as("log-validated"));

        // Publica no tópico de eventos validados
        validatedStream.to(
                KafkaTopics.VALIDATED,
                Produced.with(Serdes.String(), validatedEventSerde)
                        .withName("sink-validated-events")
        );

        // 4. Processamento de negócio: transforma ValidatedEvent em ProcessedEvent
        KStream<String, ProcessedEvent> processedStream = validatedStream
                .mapValues((key, validated) -> {
                    String payloadHash = hashService.generateHash(validated.payload());
                    Map<String, Object> result = Map.of(
                            "processingNode", "stream-processing-service",
                            "payloadHash", payloadHash
                    );
                    return ProcessedEvent.from(validated, result, payloadHash);
                }, Named.as("process-events"))
                .peek((key, processed) ->
                        log.info("Evento processado: eventId={}, status={}",
                                processed.header().eventId(), processed.status()),
                        Named.as("log-processed"));

        // Publica no tópico de eventos processados
        processedStream.to(
                KafkaTopics.PROCESSED,
                Produced.with(Serdes.String(), processedEventSerde)
                        .withName("sink-processed-events")
        );

        // 5. Avaliação de alertas: eventos de alta prioridade geram alertas
        processedStream
                .filter((key, processed) -> processed.priority().requiresImmediateAlert(),
                        Named.as("filter-high-priority"))
                .mapValues((key, processed) -> AlertEvent.create(
                        processed.header(),
                        processed.priority(),
                        "high-priority-event",
                        "Evento de alta prioridade detectado: " + processed.header().eventType(),
                        Map.of(
                                "eventId", processed.header().eventId().toString(),
                                "eventType", processed.header().eventType(),
                                "priority", processed.priority().name()
                        )
                ), Named.as("create-alerts"))
                .peek((key, alert) ->
                        log.warn("Alerta gerado: rule={}, eventId={}",
                                alert.ruleName(), alert.header().eventId()),
                        Named.as("log-alerts"))
                .to(
                        KafkaTopics.ALERTS,
                        Produced.with(Serdes.String(), alertEventSerde)
                                .withName("sink-alert-events")
                );

        // 6. Dead-Letter Queue: eventos inválidos são encaminhados à DLQ
        invalidStream
                .mapValues((key, raw) -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> originalEvent = objectMapper.convertValue(raw, Map.class);
                    return DlqEvent.create(
                            originalEvent,
                            KafkaTopics.RAW_INGEST,
                            "Evento falhou na validação de schema",
                            null,
                            0
                    );
                }, Named.as("create-dlq-events"))
                .peek((key, dlq) ->
                        log.error("Evento enviado para DLQ: dlqId={}, topic={}",
                                dlq.dlqId(), dlq.originTopic()),
                        Named.as("log-dlq"))
                .to(
                        KafkaTopics.DLQ,
                        Produced.with(Serdes.String(), dlqEventSerde)
                                .withName("sink-dlq-events")
                );
    }
}
