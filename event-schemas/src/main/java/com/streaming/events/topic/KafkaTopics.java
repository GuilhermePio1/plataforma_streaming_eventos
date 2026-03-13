package com.streaming.events.topic;

/**
 * Constantes dos tópicos Kafka da plataforma.
 * Centraliza a definição dos nomes para evitar strings duplicadas entre serviços.
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // Construtor privado para evitar instanciação de classe utilitária
    }

    // =========================================================================
    // NOMES DOS TÓPICOS (Para uso no @KafkaListener e KafkaTemplate)
    // =========================================================================

    public static final String RAW_INGEST = "events.raw.ingest";
    public static final String VALIDATED = "events.validated";
    public static final String PROCESSED = "events.processed";
    public static final String ALERTS = "events.alerts";
    public static final String DLQ = "events.dlq";

    // =========================================================================
    // METADADOS DOS TÓPICOS (Para provisionamento via KafkaAdmin / NewTopic)
    // =========================================================================

    /** Fator de replicação global definido para alta disponibilidade. */
    public static final short REPLICATION_FACTOR = 3;

    public static final int PARTITIONS_RAW_INGEST = 12;
    public static final int PARTITIONS_VALIDATED = 12;
    public static final int PARTITIONS_PROCESSED = 12;
    public static final int PARTITIONS_ALERTES = 6;
    public static final int PARTITIONS_DLQ = 3;
}
