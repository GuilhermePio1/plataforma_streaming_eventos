package com.streaming.ingestion.service;

import com.streaming.events.model.RawEvent;
import com.streaming.events.topic.KafkaTopics;
import com.streaming.ingestion.dto.EventRequest;
import com.streaming.ingestion.dto.EventResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Serviço responsável por receber eventos da API REST,
 * convertê-los em RawEvent e publicá-los no tópico Kafka de ingestão.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final KafkaTemplate<String, RawEvent> kafkaTemplate;

    public IngestionService(KafkaTemplate<String, RawEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Recebe o request da API, cria o RawEvent e publica no Kafka.
     * O Circuit Breaker protege contra falhas no broker Kafka.
     */
    @CircuitBreaker(name = "kafkaProducer", fallbackMethod = "ingestFallback")
    public CompletableFuture<EventResponse> ingest(EventRequest request) {
        RawEvent rawEvent = RawEvent.create(
                request.source(),
                request.eventType(),
                request.priority(),
                request.payload()
        );

        String partitionKey = rawEvent.header().eventId().toString();

        log.info("Publicando evento no Kafka: eventId={}, type={}, priority={}",
                rawEvent.header().eventId(),
                rawEvent.header().eventType(),
                rawEvent.priority());

        CompletableFuture<SendResult<String, RawEvent>> future =
                kafkaTemplate.send(KafkaTopics.RAW_INGEST, partitionKey, rawEvent);

        return future.thenApply(result -> {
            log.info("Evento publicado com sucesso: eventId={}, partition={}, offset={}",
                    rawEvent.header().eventId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

            return new EventResponse(
                    rawEvent.header().eventId(),
                    rawEvent.header().correlationId(),
                    rawEvent.header().timestamp(),
                    "RECEIVED"
            );
        });
    }

    /**
     * Fallback do Circuit Breaker quando o Kafka está indisponível.
     */
    @SuppressWarnings("unused")
    private CompletableFuture<EventResponse> ingestFallback(EventRequest request, Throwable throwable) {
        log.error("Circuit Breaker ativado para ingestão de eventos. Kafka indisponível: {}", throwable.getMessage());
        return CompletableFuture.failedFuture(
                new RuntimeException("Serviço de ingestão temporariamente indisponível. Tente novamente em instantes.", throwable)
        );
    }
}
