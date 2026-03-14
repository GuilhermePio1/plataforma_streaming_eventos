package com.streaming.persistence.repository;

import com.streaming.persistence.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositório para controle de idempotência de eventos processados.
 */
@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, UUID> {

    boolean existsByPayloadHash(String payloadHash);
}
