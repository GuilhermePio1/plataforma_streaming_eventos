package com.streaming.events.enums;

import lombok.Getter;

/**
 * Prioridade de negócio de um evento na plataforma.
 * Nota: Esta prioridade define regras de negócio (ex: alertas),
 * mas não altera a ordem de consumo sequencial das partições do Kafka.
 */
@Getter
public enum EventPriority {

    LOW(1, "Baixa prioridade, processamento em background ou relatórios não críticos"),
    NORMAL(2, "Prioridade padrão para o fluxo de dados da plataforma"),
    HIGH(3, "Alta prioridade, requer atenção em dashboards ou alertas de sistema"),
    CRITICAL(4, "Crítico, aciona imediatamente sistemas de alerta on-call (ex: PagerDuty, SMS)");

    private final int level;
    private final String description;

    EventPriority(int level, String description) {
        this.level = level;
        this.description = description;
    }

    /**
     * Helper para verificar se a prioridade exige atenção imediata.
     */
    public boolean requiresImmediateAlert() {
        return this.level >= HIGH.getLevel();
    }
}
