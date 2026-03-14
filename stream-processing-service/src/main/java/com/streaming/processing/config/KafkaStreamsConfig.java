package com.streaming.processing.config;

import com.streaming.events.topic.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuração do Kafka Streams com exactly-once semantics.
 * Utiliza o processamento embarcado (sem cluster externo).
 */
@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean(name = "defaultKafkaStreamsConfig")
    public KafkaStreamsConfiguration kafkaStreamsConfiguration() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationName);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 3);
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, (int) KafkaTopics.REPLICATION_FACTOR);
        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public NewTopic validatedTopic() {
        return new NewTopic(
                KafkaTopics.VALIDATED,
                KafkaTopics.PARTITIONS_VALIDATED,
                KafkaTopics.REPLICATION_FACTOR
        );
    }

    @Bean
    public NewTopic processedTopic() {
        return new NewTopic(
                KafkaTopics.PROCESSED,
                KafkaTopics.PARTITIONS_PROCESSED,
                KafkaTopics.REPLICATION_FACTOR
        );
    }

    @Bean
    public NewTopic alertsTopic() {
        return new NewTopic(
                KafkaTopics.ALERTS,
                KafkaTopics.PARTITIONS_ALERTES,
                KafkaTopics.REPLICATION_FACTOR
        );
    }

    @Bean
    public NewTopic dlqTopic() {
        return new NewTopic(
                KafkaTopics.DLQ,
                KafkaTopics.PARTITIONS_DLQ,
                KafkaTopics.REPLICATION_FACTOR
        );
    }
}
