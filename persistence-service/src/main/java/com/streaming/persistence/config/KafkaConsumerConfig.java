package com.streaming.persistence.config;

import com.streaming.events.model.ProcessedEvent;
import com.streaming.events.serde.JsonEventDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

/**
 * Configuração do consumidor Kafka para o Persistence Service.
 * Implementa retry com backoff e controle manual de offsets.
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, ProcessedEvent> consumerFactory() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "persistence-service-group",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonEventDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false
        );
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonEventDeserializer<>(ProcessedEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProcessedEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, ProcessedEvent> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, ProcessedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        // Retry: 3 tentativas com backoff de 1 segundo
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 3)));

        return factory;
    }
}
