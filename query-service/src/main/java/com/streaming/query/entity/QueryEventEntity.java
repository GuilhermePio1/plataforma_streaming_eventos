package com.streaming.query.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade JPA otimizada para leitura no Query Service.
 * Modelo de leitura separado do modelo de escrita (preparação para CQRS).
 */
@Entity
@Table(name = "events_query_view", indexes = {
        @Index(name = "idx_query_event_type", columnList = "eventType"),
        @Index(name = "idx_query_source", columnList = "source"),
        @Index(name = "idx_query_status", columnList = "status"),
        @Index(name = "idx_query_processed_at", columnList = "processedAt"),
        @Index(name = "idx_query_correlation_id", columnList = "correlationId"),
        @Index(name = "idx_query_priority", columnList = "priority")
})
public class QueryEventEntity implements Serializable {

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

    private Instant materializedAt;

    private String correlationId;

    private String payloadHash;

    protected QueryEventEntity() {
    }

    public QueryEventEntity(UUID eventId, String source, String eventType, String priority,
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
        this.materializedAt = Instant.now();
        this.correlationId = correlationId;
        this.payloadHash = payloadHash;
    }

    public UUID getEventId() { return eventId; }
    public String getSource() { return source; }
    public String getEventType() { return eventType; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public String getPayload() { return payload; }
    public String getResult() { return result; }
    public Instant getTimestamp() { return timestamp; }
    public Instant getProcessedAt() { return processedAt; }
    public Instant getMaterializedAt() { return materializedAt; }
    public String getCorrelationId() { return correlationId; }
    public String getPayloadHash() { return payloadHash; }
}
