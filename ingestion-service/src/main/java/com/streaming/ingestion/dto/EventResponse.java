package com.streaming.ingestion.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de resposta após a ingestão bem-sucedida de um evento.
 *
 * @param eventId       identificador único atribuído ao evento
 * @param correlationId identificador de correlação para rastreamento distribuído
 * @param receivedAt    instante em que o evento foi recebido pela plataforma
 * @param status        status atual do evento no pipeline
 */
public record EventResponse(
        UUID eventId,
        String correlationId,
        Instant receivedAt,
        String status
) {
}
