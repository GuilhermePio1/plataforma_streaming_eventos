package com.streaming.notification.rule;

import com.streaming.events.enums.EventPriority;
import com.streaming.events.model.AlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Regra para alertas de prioridade CRITICAL.
 * Aciona notificação imediata via canais de on-call (PagerDuty, SMS, etc.).
 */
@Component
public class CriticalPriorityRule implements AlertRule {

    private static final Logger log = LoggerFactory.getLogger(CriticalPriorityRule.class);

    @Override
    public String ruleName() {
        return "critical-priority-alert";
    }

    @Override
    public boolean matches(AlertEvent alertEvent) {
        return alertEvent.priority() == EventPriority.CRITICAL;
    }

    @Override
    public void execute(AlertEvent alertEvent) {
        log.warn("[ON-CALL] Alerta CRÍTICO disparado: rule={}, description={}, correlationId={}",
                alertEvent.ruleName(),
                alertEvent.description(),
                alertEvent.header().correlationId());

        // Ponto de extensão: integração com PagerDuty, SMS, Slack, etc.
        // pagerDutyClient.triggerIncident(alertEvent);
    }
}
