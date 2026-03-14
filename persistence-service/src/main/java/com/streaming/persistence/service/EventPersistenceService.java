package com.streaming.persistence.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streaming.events.model.ProcessedEvent;
import com.streaming.events.topic.KafkaTopics;
import com.streaming.persistence.entity.EventEntity;
import com.streaming.persistence.entity.IdempotencyRecord;
import com.streaming.persistence.repository.EventRepository;
import com.streaming.persistence.repository.IdempotencyRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço consumidor Kafka que persiste eventos processados no PostgreSQL.
 * Implementa controle de idempotência para evitar duplicatas.
 */
@Service
public class EventPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(EventPersistenceService.class);

    private final EventRepository eventRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    public EventPersistenceService(EventRepository eventRepository,
                                    IdempotencyRepository idempotencyRepository) {
        this.eventRepository = eventRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    /**
     * Consome eventos processados do Kafka e persiste no PostgreSQL.
     * Verifica idempotência antes de persistir para evitar duplicatas.
     */
    @KafkaListener(
            topics = KafkaTopics.PROCESSED,
            groupId = "persistence-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @CircuitBreaker(name = "databasePersistence", fallbackMethod = "persistFallback")
    @Transactional
    public void consumeAndPersist(ProcessedEvent processedEvent) {
        String payloadHash = processedEvent.payloadHash();

        // Controle de idempotência: verifica se o evento já foi processado
        if (idempotencyRepository.existsByPayloadHash(payloadHash)) {
            log.info("Evento duplicado detectado, ignorando: eventId={}, hash={}",
                    processedEvent.header().eventId(), payloadHash);
            return;
        }

        try {
            String payloadJson = objectMapper.writeValueAsString(processedEvent.payload());
            String resultJson = objectMapper.writeValueAsString(processedEvent.result());

            EventEntity entity = new EventEntity(
                    processedEvent.header().eventId(),
                    processedEvent.header().source(),
                    processedEvent.header().eventType(),
                    processedEvent.priority().name(),
                    processedEvent.status().name(),
                    payloadJson,
                    resultJson,
                    processedEvent.header().timestamp(),
                    processedEvent.processedAt(),
                    processedEvent.header().correlationId(),
                    payloadHash
            );

            eventRepository.save(entity);
            idempotencyRepository.save(new IdempotencyRecord(processedEvent.header().eventId(), payloadHash));

            log.info("Evento persistido com sucesso: eventId={}, type={}",
                    processedEvent.header().eventId(),
                    processedEvent.header().eventType());

        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar payload/resultado do evento: eventId={}",
                    processedEvent.header().eventId(), e);
            throw new RuntimeException("Falha na serialização do evento para persistência", e);
        }
    }

    /**
     * Fallback do Circuit Breaker quando o PostgreSQL está indisponível.
     */
    @SuppressWarnings("unused")
    private void persistFallback(ProcessedEvent processedEvent, Throwable throwable) {
        log.error("Circuit Breaker ativado para persistência. DB indisponível. eventId={}: {}",
                processedEvent.header().eventId(), throwable.getMessage());
        throw new RuntimeException("Serviço de persistência temporariamente indisponível", throwable);
    }
}
