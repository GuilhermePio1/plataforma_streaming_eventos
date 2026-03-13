package com.streaming.events.serde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Serializer Kafka genérico que converte objetos Java em JSON (bytes).
 * Configurado com suporte a Java Time API (Instant, LocalDateTime, etc.) e Records.
 */
public class JsonEventSerializer<T> implements Serializer<T> {

    private final ObjectMapper objectMapper;

    /**
     * Construtor padrão utilizado pelo client do Kafka via Reflection.
     * Instancia um ObjectMapper configurado para as melhores práticas.
     */
    public JsonEventSerializer() {
        this.objectMapper = new ObjectMapper()
                // findAndRegisterModules descobre automaticamente o JavaTimeModule,
                // Jdk8Module e ParameterNamesModule (essencial para Java Records)
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Construtor para injeção de dependência (ex: via Spring).
     */
    public JsonEventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null; // Suporte nativo a mensagens Tombstone (Log Compaction)
        }

        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException(
                    "Erro ao serializar evento do tipo " + data.getClass().getSimpleName() +
                            " para o tópico " + topic, e);
        }
    }
}