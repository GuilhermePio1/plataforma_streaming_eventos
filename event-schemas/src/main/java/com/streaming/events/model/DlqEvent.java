package com.streaming.events.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Envelope para eventos encaminhados à Dead-Letter Queue (DLQ).
 * Contém o evento original com metadados completos do erro.
 * Publicado no tópico {@code events.dlq}.
 *
 * @param dlqId          identificador único do registro na DLQ
 * @param originalEvent  evento original que falhou no processamento
 * @param originTopic    tópico Kafka de origem do evento
 * @param errorMessage   mensagem de erro descritiva
 * @param stackTrace     stack trace completa da exceção
 * @param retryCount     número de tentativas realizadas antes do envio à DLQ (pode ser zero em erros fatais)
 * @param failedAt       instante em que a falha definitiva ocorreu
 */
public record DlqEvent(

        @NotNull UUID dlqId,
        @NotNull Map<String, Object> originalEvent,
        @NotBlank String originTopic,
        @NotBlank String errorMessage,
        String stackTrace,
        @PositiveOrZero int retryCount,
        @NotNull Instant failedAt
) {

    // Construtor compacto para garantir a imutabilidade e consistência
    public DlqEvent {
        Objects.requireNonNull(dlqId, "O ID da DLQ (dlqId) não pode ser nulo");
        Objects.requireNonNull(originalEvent, "O evento original não pode ser nulo");
        Objects.requireNonNull(originTopic, "O tópico de origem não pode ser nulo");
        Objects.requireNonNull(errorMessage, "A mensagem de erro não pode ser nula");
        Objects.requireNonNull(failedAt, "O instante da falha (failedAt) não pode ser nulo");

        if (originTopic.isBlank()) throw new IllegalArgumentException("O tópico de origem não pode ser vazio");
        if (errorMessage.isBlank()) throw new IllegalArgumentException("A mensagem de erro não pode ser vazia");
        if (retryCount < 0) throw new IllegalArgumentException("O número de tentativas não pode ser negativo");

        // Garante que o evento original não sofra mutação após encapsulado
        originalEvent = originalEvent().isEmpty()
                ? Map.of()
                : Map.copyOf(originalEvent);
    }

    /**
     * Factory method para encapsular um evento que falhou no processamento.
     */
    public static DlqEvent create(Map<String, Object> originalEvent, String originTopic,
                                  String errorMessage, String stackTrace, int retryCount) {
        return new DlqEvent(
                UUID.randomUUID(),
                originalEvent,
                originTopic,
                errorMessage,
                stackTrace,
                retryCount,
                Instant.now()
        );
    }
}
