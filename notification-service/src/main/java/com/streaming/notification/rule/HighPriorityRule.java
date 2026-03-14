package com.streaming.notification.rule;

import com.streaming.events.enums.EventPriority;
import com.streaming.events.model.AlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Regra para alertas de prioridade HIGH.
 * Envia notificação para dashboards e canais de monitoramento.
 */
@Component
public class HighPriorityRule implements AlertRule {

    private static final Logger log = LoggerFactory.getLogger(HighPriorityRule.class);

    @Override
    public String ruleName() {
        return "high-priority-alert";
    }

    @Override
    public boolean matches(AlertEvent alertEvent) {
        return alertEvent.priority() == EventPriority.HIGH;
    }

    @Override
    public void execute(AlertEvent alertEvent) {
        log.warn("[DASHBOARD] Alerta de alta prioridade: rule={}, description={}, correlationId={}",
                alertEvent.ruleName(),
                alertEvent.description(),
                alertEvent.header().correlationId());

        // Ponto de extensão: integração com Slack, email, dashboard, etc.
        // slackNotifier.sendToChannel("#alerts", alertEvent);
    }
}
