package com.streaming.notification.rule;

import com.streaming.events.model.AlertEvent;

/**
 * Interface para regras de avaliação de alertas.
 * Cada implementação define uma regra configurável que determina
 * como o alerta deve ser notificado.
 */
public interface AlertRule {

    /**
     * Nome identificador da regra.
     */
    String ruleName();

    /**
     * Avalia se esta regra se aplica ao alerta recebido.
     */
    boolean matches(AlertEvent alertEvent);

    /**
     * Executa a ação de notificação definida pela regra.
     */
    void execute(AlertEvent alertEvent);
}
