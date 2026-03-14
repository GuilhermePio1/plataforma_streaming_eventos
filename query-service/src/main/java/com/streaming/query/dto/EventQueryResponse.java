package com.streaming.query.dto;

import com.streaming.query.entity.QueryEventEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de resposta para consultas de eventos.
 * Projeta apenas os campos necessários para a API REST.
 */
public record EventQueryResponse(
        UUID eventId,
        String source,
        String eventType,
        String priority,
        String status,
        String payload,
        String result,
        Instant timestamp,
        Instant processedAt,
        String correlationId
) {

    public static EventQueryResponse from(QueryEventEntity entity) {
        return new EventQueryResponse(
                entity.getEventId(),
                entity.getSource(),
                entity.getEventType(),
                entity.getPriority(),
                entity.getStatus(),
                entity.getPayload(),
                entity.getResult(),
                entity.getTimestamp(),
                entity.getProcessedAt(),
                entity.getCorrelationId()
        );
    }
}
