package com.streaming.events.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Cabeçalho comum a todos os eventos da plataforma.
 * Contém metadados de rastreabilidade, origem e correlação.
 */
public record EventHeader(
        @NotNull UUID eventId,
        @NotBlank String source,
        @NotBlank String eventType,
        @NotNull Instant timestamp,
        String correlationId,
        Map<String, String> metadata
) {

    // Construtor compacto: executado sempre que o record é instanciado
    public EventHeader {
        Objects.requireNonNull(eventId, "eventId não pode ser nulo");
        Objects.requireNonNull(source, "source não pode ser nulo");
        Objects.requireNonNull(eventType, "eventType não pode ser nulo");
        Objects.requireNonNull(timestamp, "timestamp não pode ser nulo");

        // Garante que a string não seja vazia (equivalente ao @NotBlank programático)
        if (source.isBlank()) throw new IllegalArgumentException("source não pode ser vazio");
        if (eventType.isBlank()) throw new IllegalArgumentException("eventType não pode ser vazio");

        // Garante a imutabilidade profunda do Record
        metadata = (metadata == null || metadata.isEmpty())
                ? Map.of()
                : Map.copyOf(metadata);
    }

    /**
     * Cria um novo cabeçalho de evento inicial (origem da cadeia).
     */
    public static EventHeader create(String source, String eventType) {
        return new EventHeader(
                UUID.randomUUID(),
                source,
                eventType,
                Instant.now(),
                UUID.randomUUID().toString(), // Use com cuidado para não sobrescrever o MDC/OpenTelemetry
                Map.of()
        );
    }

    /**
     * Cria um cabeçalho propagando um correlationId existente.
     */
    public static EventHeader create(String source, String eventType, String correlationId, Map<String, String> metadata) {
        return new EventHeader(
                UUID.randomUUID(),
                source,
                eventType,
                Instant.now(),
                correlationId,
                metadata
        );
    }
}
