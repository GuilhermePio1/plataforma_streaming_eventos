package com.streaming.events.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * Serde (Serializer + Deserializer) Kafka para uso com Kafka Streams.
 * Facilita a configuração de state stores e operações de stream.
 *
 * <p>Exemplo de uso:</p>
 * <pre>{@code
 * Serde<RawEvent> rawEventSerde = new JsonEventSerde<>(RawEvent.class);
 * StreamsBuilder builder = new StreamsBuilder();
 * builder.stream("events.raw.ingest", Consumed.with(Serdes.String(), rawEventSerde));
 * }</pre>
 */
public class JsonEventSerde<T> implements Serde<T> {

    private final JsonEventSerializer<T> serializer;
    private final JsonEventDeserializer<T> deserializer;

    /**
     * Construtor padrão que inicializa as classes de serialização com
     * um ObjectMapper autogerenciado.
     */
    public JsonEventSerde(Class<T> targetType) {
        this.serializer = new JsonEventSerializer<>();
        this.deserializer = new JsonEventDeserializer<>(targetType);
    }

    /**
     * Construtor que permite a injeção de um ObjectMapper customizado
     * (ex: bean global do Spring Boot).
     */
    public JsonEventSerde(Class<T> targetType, ObjectMapper objectMapper) {
        this.serializer = new JsonEventSerializer<>(objectMapper);
        this.deserializer = new JsonEventDeserializer<>(targetType, objectMapper);
    }

    @Override
    public Serializer<T> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<T> deserializer() {
        return deserializer;
    }

    /**
     * Delega a configuração recebida do Kafka Streams para os componentes internos.
     */
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        serializer.configure(configs, isKey);
        deserializer.configure(configs, isKey);
    }

    /**
     * Delega o encerramento do ciclo de vida para os componentes internos.
     */
    @Override
    public void close() {
        serializer.close();
        deserializer.close();
    }
}
