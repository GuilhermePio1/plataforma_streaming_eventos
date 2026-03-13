package com.streaming.events.enums;

import lombok.Getter;

/**
 * Status do ciclo de vida de um evento na plataforma de streaming.
 */
@Getter
public enum EventStatus {

    // --- Fluxo Feliz ---
    RECEIVED("Evento recebido pelo Ingestion Service", false),
    VALIDATED("Evento validado contra os schemas de entrada", false),
    ENRICHED("Evento enriquecido com dados adicionais", false),
    PROCESSED("Processamento de negócio concluído via Kafka Streams", false),
    PERSISTED("Evento persistido no banco de dados relacional", true),
    NOTIFIED("Notificação/Alerta disparado com sucesso", true),

    // --- Fluxo de Exceção e Resiliência ---
    REJECTED("Evento rejeitado (contrato inválido ou erro de negócio irreparável)", true),
    ERROR_RETRYING("Erro transitório detectado, evento em fila de retentativa", false),
    DEAD_LETTER("Tentativas esgotadas, evento enviado para a DLQ", true),

    // --- Fluxo de Reprocessamento ---
    RECOVERED("Evento reprocessado com sucesso a partir da DLQ", true);

    private final String description;
    private final boolean terminal; // Indica se o ciclo de vida deste evento encerrou

    EventStatus(String description, boolean terminal) {
        this.description = description;
        this.terminal = terminal;
    }

    /**
     * Verifica se o evento chegou ao fim do seu ciclo de vida.
     */
    public boolean isTerminal() {
        return terminal;
    }
}
