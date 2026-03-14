package com.streaming.persistence.repository;

import com.streaming.persistence.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repositório JPA para acesso aos eventos persistidos no PostgreSQL.
 */
@Repository
public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    List<EventEntity> findByEventType(String eventType);

    List<EventEntity> findBySource(String source);

    List<EventEntity> findByStatus(String status);

    List<EventEntity> findByCorrelationId(String correlationId);

    List<EventEntity> findByProcessedAtBetween(Instant start, Instant end);

    boolean existsByPayloadHash(String payloadHash);
}
