package com.streaming.events.model;

import com.streaming.events.enums.EventPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.Objects;

/**
 * Evento bruto recebido pelo Ingestion Service.
 * Publicado no tópico {@code events.raw.ingest}.
 *
 * @param header   cabeçalho com metadados do evento
 * @param priority prioridade de processamento
 * @param payload  dados brutos do evento em formato chave-valor
 */
public record RawEvent(
        @Valid @NotNull EventHeader header,
        @NotNull EventPriority priority,
        @NotNull Map<String, Object> payload
) {

    // Construtor compacto para garantir validações e imutabilidade profunda
    public RawEvent {
        Objects.requireNonNull(header, "O cabeçalho do evento (header) não pode ser nulo");
        Objects.requireNonNull(priority, "A prioridade do evento (priority) não pode ser nula");

        // Garante que o payload não seja nulo e cria uma cópia imutável
        payload = (payload == null || payload.isEmpty())
                ? Map.of()
                : Map.copyOf(payload);
    }

    /**
     * Factory method para facilitar a criação de um novo evento bruto.
     */
    public static RawEvent create(String source, String eventType, EventPriority priority, Map<String, Object> payload) {
        return new RawEvent(
                EventHeader.create(source, eventType),
                priority,
                payload
        );
    }
}
