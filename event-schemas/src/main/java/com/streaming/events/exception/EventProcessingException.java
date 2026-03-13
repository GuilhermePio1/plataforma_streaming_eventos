package com.streaming.events.exception;

import java.util.UUID;

/**
 * Exceção base para falhas no processamento de eventos.
 * Carrega o eventId para rastreabilidade e correlação com a DLQ.
 */
public abstract class EventProcessingException extends RuntimeException {

    private final UUID eventId;

    // Define se o erro é transitório (true) ou se é um Poison Pill/erro fatal (false)
    private final boolean retryable;

    public EventProcessingException(UUID eventId, String message, boolean retryable) {
        super(message);
        this.eventId = eventId;
        this.retryable = retryable;
    }

    public EventProcessingException(UUID eventId, String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.eventId = eventId;
        this.retryable = retryable;
    }

    public UUID getEventId() {
        return eventId;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
