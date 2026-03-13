package com.streaming.events.model;

import com.streaming.events.enums.EventPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Evento de alerta gerado quando uma regra configurável é acionada.
 * Publicado no tópico {@code events.alerts}.
 *
 * @param header       cabeçalho do evento de alerta
 * @param priority     prioridade do alerta
 * @param ruleName     nome da regra que disparou o alerta
 * @param description  descrição legível do alerta
 * @param sourceEvent  dados resumidos do evento que originou o alerta
 * @param triggeredAt  instante em que o alerta foi disparado
 */
public record AlertEvent(

        @Valid @NotNull EventHeader header,
        @NotNull EventPriority priority,
        @NotBlank String ruleName,
        @NotBlank String description,
        @NotNull Map<String, Object> sourceEvent,
        @NotNull Instant triggeredAt
) {

    // Construtor compacto para garantir validações e imutabilidade
    public AlertEvent {
        Objects.requireNonNull(header, "O cabeçalho (header) não pode ser nulo");
        Objects.requireNonNull(priority, "A prioridade (priority) não pode ser nula");
        Objects.requireNonNull(ruleName, "O nome da regra (ruleName) não pode ser nulo");
        Objects.requireNonNull(description, "A descrição não pode ser nula");
        Objects.requireNonNull(triggeredAt, "O instante do disparo (triggeredAt) não pode ser nulo");

        if (ruleName.isBlank()) throw new IllegalArgumentException("O nome da regra não pode ser vazio");
        if (description.isBlank()) throw new IllegalArgumentException("A descrição não pode ser vazia");

        // Cópia defensiva do evento de origem
        sourceEvent = (sourceEvent == null || sourceEvent.isEmpty())
                ? Map.of()
                : Map.copyOf(sourceEvent);
    }

    /**
     * Factory method modificado para exigir o cabeçalho original,
     * garantindo a propagação do correlationId para os traces distribuídos.
     */
    public static AlertEvent create(
            EventHeader originalHeader, // Passamos o header original para manter o rastro
            EventPriority priority,
            String ruleName,
            String description,
            Map<String, Object> sourceEvent
    ) {

        // Criamos o novo header propagando o trace e salvando o ID do evento causador
        EventHeader alertHeader = EventHeader.create(
                "stream-processing-service",
                "ALERT",
                originalHeader.correlationId(), // Mantém a cadeia de logs intacta
                Map.of("causedByEventId", originalHeader.eventId().toString())
        );

        return new AlertEvent(
                alertHeader,
                priority,
                ruleName,
                description,
                sourceEvent,
                Instant.now()
        );
    }
}
