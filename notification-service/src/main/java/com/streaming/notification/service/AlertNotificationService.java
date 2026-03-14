package com.streaming.notification.service;

import com.streaming.events.model.AlertEvent;
import com.streaming.events.topic.KafkaTopics;
import com.streaming.notification.rule.AlertRule;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço consumidor Kafka que avalia regras de alerta e dispara notificações.
 * Utiliza o padrão Strategy para permitir regras configuráveis e extensíveis.
 */
@Service
public class AlertNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AlertNotificationService.class);

    private final List<AlertRule> alertRules;

    public AlertNotificationService(List<AlertRule> alertRules) {
        this.alertRules = alertRules;
        log.info("Notification Service inicializado com {} regras de alerta", alertRules.size());
    }

    /**
     * Consome alertas do Kafka e avalia cada regra configurada.
     * Executa todas as regras que correspondem ao alerta recebido.
     */
    @KafkaListener(
            topics = KafkaTopics.ALERTS,
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @CircuitBreaker(name = "notificationDispatch", fallbackMethod = "notifyFallback")
    public void consumeAndNotify(AlertEvent alertEvent) {
        log.info("Alerta recebido: rule={}, priority={}, correlationId={}",
                alertEvent.ruleName(),
                alertEvent.priority(),
                alertEvent.header().correlationId());

        int matchedRules = 0;

        for (AlertRule rule : alertRules) {
            if (rule.matches(alertEvent)) {
                rule.execute(alertEvent);
                matchedRules++;
            }
        }

        if (matchedRules == 0) {
            log.info("Nenhuma regra correspondente para o alerta: rule={}, priority={}",
                    alertEvent.ruleName(), alertEvent.priority());
        } else {
            log.info("Alerta processado: {} regras acionadas para eventId={}",
                    matchedRules, alertEvent.header().eventId());
        }
    }

    /**
     * Fallback do Circuit Breaker quando o serviço de notificação falha.
     */
    @SuppressWarnings("unused")
    private void notifyFallback(AlertEvent alertEvent, Throwable throwable) {
        log.error("Circuit Breaker ativado para notificações. alertId={}: {}",
                alertEvent.header().eventId(), throwable.getMessage());
        // Em produção: persistir alerta em fila de retry ou tabela de fallback
    }
}
