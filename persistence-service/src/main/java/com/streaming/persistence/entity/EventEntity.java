package com.streaming.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade JPA que representa um evento persistido no PostgreSQL.
 * Utiliza JSONB para armazenamento flexível de payload e resultado.
 */
@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_events_event_type", columnList = "eventType"),
        @Index(name = "idx_events_source", columnList = "source"),
        @Index(name = "idx_events_status", columnList = "status"),
        @Index(name = "idx_events_processed_at", columnList = "processedAt"),
        @Index(name = "idx_events_correlation_id", columnList = "correlationId")
})
public class EventEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String priority;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "jsonb")
    private String payload;

    @Column(columnDefinition = "jsonb")
    private String result;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private Instant processedAt;

    private Instant persistedAt;

    private String correlationId;

    @Column(nullable = false, unique = true)
    private String payloadHash;

    protected EventEntity() {
        // Construtor padrão exigido pelo JPA
    }

    public EventEntity(UUID eventId, String source, String eventType, String priority,
                        String status, String payload, String result, Instant timestamp,
                        Instant processedAt, String correlationId, String payloadHash) {
        this.eventId = eventId;
        this.source = source;
        this.eventType = eventType;
        this.priority = priority;
        this.status = status;
        this.payload = payload;
        this.result = result;
        this.timestamp = timestamp;
        this.processedAt = processedAt;
        this.persistedAt = Instant.now();
        this.correlationId = correlationId;
        this.payloadHash = payloadHash;
    }

    // Getters

    public UUID getEventId() { return eventId; }
    public String getSource() { return source; }
    public String getEventType() { return eventType; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public String getPayload() { return payload; }
    public String getResult() { return result; }
    public Instant getTimestamp() { return timestamp; }
    public Instant getProcessedAt() { return processedAt; }
    public Instant getPersistedAt() { return persistedAt; }
    public String getCorrelationId() { return correlationId; }
    public String getPayloadHash() { return payloadHash; }

    public void setStatus(String status) { this.status = status; }
    public void setPersistedAt(Instant persistedAt) { this.persistedAt = persistedAt; }
}
