package com.streaming.query.repository;

import com.streaming.query.entity.QueryEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repositório JPA para consultas otimizadas no modelo de leitura.
 */
@Repository
public interface QueryEventRepository extends JpaRepository<QueryEventEntity, UUID> {

    Page<QueryEventEntity> findByEventType(String eventType, Pageable pageable);

    Page<QueryEventEntity> findBySource(String source, Pageable pageable);

    Page<QueryEventEntity> findByStatus(String status, Pageable pageable);

    Page<QueryEventEntity> findByPriority(String priority, Pageable pageable);

    List<QueryEventEntity> findByCorrelationId(String correlationId);

    Page<QueryEventEntity> findByProcessedAtBetween(Instant start, Instant end, Pageable pageable);

    Page<QueryEventEntity> findBySourceAndEventType(String source, String eventType, Pageable pageable);
}
