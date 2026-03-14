package com.streaming.ingestion.config;

import com.streaming.events.model.RawEvent;
import com.streaming.events.serde.JsonEventSerializer;
import com.streaming.events.topic.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

/**
 * Configuração do produtor Kafka para o Ingestion Service.
 * Utiliza exactly-once semantics via transações idempotentes.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, RawEvent> producerFactory() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonEventSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                ProducerConfig.RETRIES_CONFIG, 3,
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1
        );
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, RawEvent> kafkaTemplate(ProducerFactory<String, RawEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public NewTopic rawIngestTopic() {
        return new NewTopic(
                KafkaTopics.RAW_INGEST,
                KafkaTopics.PARTITIONS_RAW_INGEST,
                KafkaTopics.REPLICATION_FACTOR
        );
    }
}
