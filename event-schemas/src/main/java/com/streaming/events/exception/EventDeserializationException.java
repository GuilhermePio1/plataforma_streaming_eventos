package com.streaming.events.exception;

/**
 * Exceção lançada quando a desserialização de um evento Kafka falha (Poison Pill).
 * Sendo um erro de formatação/parse, é considerado FATAL e não deve sofrer retentativas.
 */
public class EventDeserializationException extends RuntimeException {

    private final String topic;

    // Armazena o conteúdo bruto (JSON quebrado, XML, etc.) que causou a falha
    private final String rawPayload;

    public EventDeserializationException(String topic, String rawPayload, String message, Throwable cause) {
        super(message, cause);
        this.topic = topic;
        this.rawPayload = rawPayload;
    }

    public String getTopic() {
        return topic;
    }

    public String getRawPayload() {
        return rawPayload;
    }
}
