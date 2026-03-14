package com.streaming.query.service;

import com.streaming.query.dto.EventQueryResponse;
import com.streaming.query.entity.QueryEventEntity;
import com.streaming.query.repository.QueryEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de consulta de eventos com cache Redis.
 * Fornece endpoints de leitura otimizados para o modelo de consulta.
 */
@Service
public class EventQueryService {

    private static final Logger log = LoggerFactory.getLogger(EventQueryService.class);

    private final QueryEventRepository repository;

    public EventQueryService(QueryEventRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = "events", key = "#eventId")
    public Optional<EventQueryResponse> findById(UUID eventId) {
        log.debug("Consultando evento por ID: {}", eventId);
        return repository.findById(eventId).map(EventQueryResponse::from);
    }

    @Cacheable(cacheNames = "events-by-type", key = "#eventType + '-' + #pageable.pageNumber")
    public Page<EventQueryResponse> findByEventType(String eventType, Pageable pageable) {
        log.debug("Consultando eventos por tipo: {}", eventType);
        return repository.findByEventType(eventType, pageable).map(EventQueryResponse::from);
    }

    public Page<EventQueryResponse> findBySource(String source, Pageable pageable) {
        log.debug("Consultando eventos por source: {}", source);
        return repository.findBySource(source, pageable).map(EventQueryResponse::from);
    }

    public Page<EventQueryResponse> findByStatus(String status, Pageable pageable) {
        log.debug("Consultando eventos por status: {}", status);
        return repository.findByStatus(status, pageable).map(EventQueryResponse::from);
    }

    public Page<EventQueryResponse> findByPriority(String priority, Pageable pageable) {
        log.debug("Consultando eventos por prioridade: {}", priority);
        return repository.findByPriority(priority, pageable).map(EventQueryResponse::from);
    }

    public List<EventQueryResponse> findByCorrelationId(String correlationId) {
        log.debug("Consultando eventos por correlationId: {}", correlationId);
        return repository.findByCorrelationId(correlationId)
                .stream()
                .map(EventQueryResponse::from)
                .toList();
    }

    public Page<EventQueryResponse> findByDateRange(Instant start, Instant end, Pageable pageable) {
        log.debug("Consultando eventos por período: {} a {}", start, end);
        return repository.findByProcessedAtBetween(start, end, pageable).map(EventQueryResponse::from);
    }

    public Page<EventQueryResponse> findAll(Pageable pageable) {
        log.debug("Consultando todos os eventos, página: {}", pageable.getPageNumber());
        return repository.findAll(pageable).map(EventQueryResponse::from);
    }
}
