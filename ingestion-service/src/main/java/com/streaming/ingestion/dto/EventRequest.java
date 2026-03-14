package com.streaming.ingestion.dto;

import com.streaming.events.enums.EventPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * DTO de entrada para a API REST de ingestão de eventos.
 * Validado via Bean Validation antes de ser convertido em RawEvent.
 *
 * @param source    sistema de origem do evento
 * @param eventType tipo do evento (ex: "ORDER_CREATED", "PAYMENT_RECEIVED")
 * @param priority  prioridade de processamento do evento
 * @param payload   dados brutos do evento em formato chave-valor
 */
public record EventRequest(

        @NotBlank(message = "O campo 'source' é obrigatório")
        String source,

        @NotBlank(message = "O campo 'eventType' é obrigatório")
        String eventType,

        @NotNull(message = "O campo 'priority' é obrigatório")
        EventPriority priority,

        @NotNull(message = "O campo 'payload' é obrigatório")
        Map<String, Object> payload
) {
}
