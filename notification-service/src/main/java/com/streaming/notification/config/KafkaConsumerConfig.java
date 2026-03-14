package com.streaming.notification.config;

import com.streaming.events.model.AlertEvent;
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
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.Map;

/**
 * Configuração do consumidor Kafka para o Notification Service.
 * Implementa retry com backoff exponencial conforme especificado na arquitetura.
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, AlertEvent> consumerFactory() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "notification-service-group",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonEventDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false
        );
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonEventDeserializer<>(AlertEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AlertEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, AlertEvent> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, AlertEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(2);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        // Retry com backoff exponencial: 1s → 5s → 30s (3 tentativas)
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 5.0);
        backOff.setMaxAttempts(3);
        factory.setCommonErrorHandler(new DefaultErrorHandler(backOff));

        return factory;
    }
}
