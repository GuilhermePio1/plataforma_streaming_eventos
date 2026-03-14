package com.streaming.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tabela de controle de idempotência para evitar processamento duplicado.
 * Utiliza event_id + payload_hash para garantir unicidade.
 */
@Entity
@Table(name = "idempotency_control", indexes = {
        @Index(name = "idx_idempotency_payload_hash", columnList = "payloadHash", unique = true)
})
public class IdempotencyRecord {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, unique = true)
    private String payloadHash;

    @Column(nullable = false)
    private Instant processedAt;

    protected IdempotencyRecord() {
        // Construtor padrão exigido pelo JPA
    }

    public IdempotencyRecord(UUID eventId, String payloadHash) {
        this.eventId = eventId;
        this.payloadHash = payloadHash;
        this.processedAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public String getPayloadHash() { return payloadHash; }
    public Instant getProcessedAt() { return processedAt; }
}
