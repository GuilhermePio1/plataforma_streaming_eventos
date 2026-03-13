package com.streaming.events.model;

import com.streaming.events.enums.EventPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Evento validado e enriquecido pelo Stream Processing Service.
 * Publicado no tópico {@code events.validated}.
 *
 * @param header        cabeçalho original do evento
 * @param priority      prioridade de processamento
 * @param payload       dados do evento após enriquecimento
 * @param validatedAt   instante em que o evento foi validado
 * @param schemaVersion versão do schema utilizado na validação
 */
public record ValidatedEvent(

        @Valid @NotNull EventHeader header,
        @NotNull EventPriority priority,
        @NotNull Map<String, Object> payload,
        @NotNull Instant validatedAt,
        @NotBlank String schemaVersion
) {

    // Construtor compacto para garantir a imutabilidade profunda e blindar o estado
    public ValidatedEvent {
        Objects.requireNonNull(header, "O cabeçalho (header) não pode ser nulo");
        Objects.requireNonNull(priority, "A prioridade (priority) não pode ser nula");
        Objects.requireNonNull(validatedAt, "O instante de validação (validatedAt) não pode ser nulo");
        Objects.requireNonNull(schemaVersion, "A versão do schema (schemaVersion) não pode ser nula");

        if (schemaVersion.isBlank()) {
            throw new IllegalArgumentException("A versão do schema não pode ser vazia");
        }

        // Garante que o payload não seja nulo e cria uma cópia imutável
        payload = (payload == null || payload.isEmpty())
                ? Map.of()
                : Map.copyOf(payload);
    }

    /**
     * Factory method para um evento que foi validado, mas não precisou ter seu payload modificado.
     */
    public static ValidatedEvent from(RawEvent raw, String schemaVersion) {
        return new ValidatedEvent(
                raw.header(),
                raw.priority(),
                raw.payload(),
                Instant.now(),
                schemaVersion
        );
    }

    /**
     * Factory method para um evento que foi validado e teve seu payload modificado (enriquecido).
     */
    public static ValidatedEvent fromEnriched(RawEvent raw, Map<String, Object> enrichedPayload, String schemaVersion) {
        return new ValidatedEvent(
                raw.header(),
                raw.priority(),
                enrichedPayload,
                Instant.now(),
                schemaVersion
        );
    }
}
