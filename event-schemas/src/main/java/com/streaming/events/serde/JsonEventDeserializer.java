package com.streaming.events.serde;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streaming.events.exception.EventDeserializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Deserializer Kafka genérico que converte bytes JSON em objetos Java.
 * Configurado com suporte a Java Time API, Java Records e tolerância a campos desconhecidos
 * para permitir evolução de schema sem quebrar consumidores.
 */
public class JsonEventDeserializer<T> implements Deserializer<T> {

    private final ObjectMapper objectMapper;
    private final Class<T> targetType;

    /**
     * Construtor padrão com configuração autogerenciada do Jackson.
     */
    public JsonEventDeserializer(Class<T> targetType) {
        this.targetType = targetType;
        this.objectMapper = new ObjectMapper()
                // Descobre módulos essenciais (JavaTimeModule, Jdk8Module e ParameterNamesModule para Records)
                .findAndRegisterModules()
                // Permite a evolução do schema (ignora campos novos no JSON)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * Construtor para injeção de um ObjectMapper customizado/global.
     */
    public JsonEventDeserializer(Class<T> targetType, ObjectMapper objectMapper) {
        this.targetType = targetType;
        this.objectMapper = objectMapper;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        // Suporte a mensagens de exclusão (Tombstones / Log Compaction)
        if (data == null || data.length == 0) {
            return null;
        }

        try {
            return objectMapper.readValue(data, targetType);
        } catch (IOException e) {
            // Converte os bytes malformados em String UTF-8 para análise e envio à DLQ
            String rawPayload = new String(data, StandardCharsets.UTF_8);

            // Lança a exceção atualizada, passando o rawPayload!
            throw new EventDeserializationException(
                    topic,
                    rawPayload,
                    "Erro ao deserializar evento do tópico " + topic + " para a classe " + targetType.getSimpleName(),
                    e
            );
        }
    }
}
