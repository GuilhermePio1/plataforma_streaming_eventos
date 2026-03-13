package com.streaming.events.model;

import com.streaming.events.enums.EventPriority;
import com.streaming.events.enums.EventStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Evento processado e pronto para persistência e consulta.
 * Publicado no tópico {@code events.processed}.
 *
 * @param header       cabeçalho original do evento
 * @param priority     prioridade de processamento
 * @param status       status final do processamento
 * @param payload      dados do evento após transformação
 * @param result       resultado do processamento (agregações, cálculos, etc.)
 * @param processedAt  instante em que o processamento foi concluído
 * @param payloadHash  hash do payload para controle de idempotência
 */
public record ProcessedEvent(

        @Valid @NotNull EventHeader header,
        @NotNull EventPriority priority,
        @NotNull EventStatus status,
        @NotNull Map<String, Object> payload,
        Map<String, Object> result,
        @NotNull Instant processedAt,
        @NotBlank String payloadHash
) {

    // Construtor compacto para garantir validações e imutabilidade profunda
    public ProcessedEvent {
        Objects.requireNonNull(header, "O cabeçalho (header) não pode ser nulo");
        Objects.requireNonNull(priority, "A prioridade (priority) não pode ser nula");
        Objects.requireNonNull(status, "O status não pode ser nulo");
        Objects.requireNonNull(processedAt, "O instante de processamento (processedAt) não pode ser nulo");
        Objects.requireNonNull(payloadHash, "O hash do payload (payloadHash) não pode ser nulo");

        if (payloadHash.isBlank()) {
            throw new IllegalArgumentException("O hash do payload não pode ser vazio");
        }

        // Garante imutabilidade e evita null pointers nos consumidores
        payload = (payload == null || payload.isEmpty())
                ? Map.of()
                : Map.copyOf(payload);

        result = (result == null || result.isEmpty())
                ? Map.of()
                : Map.copyOf(result);
    }

    /**
     * Factory method para o caminho feliz de processamento com sucesso.
     */
    public static ProcessedEvent from(ValidatedEvent validated, Map<String, Object> result, String payloadHash) {
        return new ProcessedEvent(
                validated.header(),
                validated.priority(),
                EventStatus.PROCESSED,
                validated.payload(), // Aqui assumimos que a transformação de payload já ocorreu ou foi mantida
                result,
                Instant.now(),
                payloadHash
        );
    }
}
