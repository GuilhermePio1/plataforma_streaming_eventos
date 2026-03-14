package com.streaming.query.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streaming.events.model.ProcessedEvent;
import com.streaming.events.topic.KafkaTopics;
import com.streaming.query.entity.QueryEventEntity;
import com.streaming.query.repository.QueryEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço consumidor Kafka que materializa eventos processados
 * no modelo de leitura do Query Service.
 * Invalida o cache Redis quando novos eventos são materializados.
 */
@Service
public class EventMaterializationService {

    private static final Logger log = LoggerFactory.getLogger(EventMaterializationService.class);

    private final QueryEventRepository queryEventRepository;
    private final ObjectMapper objectMapper;

    public EventMaterializationService(QueryEventRepository queryEventRepository) {
        this.queryEventRepository = queryEventRepository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @KafkaListener(
            topics = KafkaTopics.PROCESSED,
            groupId = "query-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    @CacheEvict(cacheNames = {"events", "events-by-type"}, allEntries = true)
    public void materialize(ProcessedEvent processedEvent) {
        if (queryEventRepository.existsById(processedEvent.header().eventId())) {
            log.debug("Evento já materializado, ignorando: eventId={}",
                    processedEvent.header().eventId());
            return;
        }

        try {
            String payloadJson = objectMapper.writeValueAsString(processedEvent.payload());
            String resultJson = objectMapper.writeValueAsString(processedEvent.result());

            QueryEventEntity entity = new QueryEventEntity(
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
                    processedEvent.payloadHash()
            );

            queryEventRepository.save(entity);

            log.info("Evento materializado para consulta: eventId={}, type={}",
                    processedEvent.header().eventId(),
                    processedEvent.header().eventType());

        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar evento para materialização: eventId={}",
                    processedEvent.header().eventId(), e);
            throw new RuntimeException("Falha na materialização do evento", e);
        }
    }
}
