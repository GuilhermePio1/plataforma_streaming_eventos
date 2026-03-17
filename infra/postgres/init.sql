-- ============================================================
-- Script de Inicialização do PostgreSQL
-- Plataforma de Streaming de Eventos
-- ============================================================

-- Habilita extensões úteis
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ============================================================
-- Schema principal para dados de eventos
-- ============================================================
CREATE SCHEMA IF NOT EXISTS events;

-- ============================================================
-- Tabela de controle de idempotência
-- ============================================================
CREATE TABLE IF NOT EXISTS events.idempotency_control (
    event_id        UUID PRIMARY KEY,
    payload_hash    VARCHAR(64) NOT NULL,
    topic_origin    VARCHAR(100) NOT NULL,
    processed_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
    -- CONSTRAINT uq_event_hash UNIQUE (event_id, payload_hash) -- REMOVIDA: Redundante com a PK
);

CREATE INDEX IF NOT EXISTS idx_idempotency_processed_at
    ON events.idempotency_control (processed_at);

-- ============================================================
-- Tabela principal de eventos processados
-- ============================================================
CREATE TABLE IF NOT EXISTS events.processed_events (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    event_id        UUID NOT NULL,
    source          VARCHAR(255) NOT NULL,
    event_type      VARCHAR(255) NOT NULL,
    priority        VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    status          VARCHAR(30) NOT NULL,

    -- CORREÇÃO CRÍTICA: Alterado de UUID para VARCHAR para suportar Trace IDs do OpenTelemetry (W3C)
    correlation_id  VARCHAR(255),

    payload         JSONB NOT NULL,
    result          JSONB,
    metadata        JSONB,
    payload_hash    VARCHAR(64),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    validated_at    TIMESTAMP WITH TIME ZONE,
    processed_at    TIMESTAMP WITH TIME ZONE,
    persisted_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Partições para os próximos meses (gerar automaticamente em produção)
CREATE TABLE IF NOT EXISTS events.processed_events_2026_03
    PARTITION OF events.processed_events
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

CREATE TABLE IF NOT EXISTS events.processed_events_2026_04
    PARTITION OF events.processed_events
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

CREATE TABLE IF NOT EXISTS events.processed_events_2026_05
    PARTITION OF events.processed_events
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');

CREATE TABLE IF NOT EXISTS events.processed_events_2026_06
    PARTITION OF events.processed_events
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

-- Partição default para eventos fora do range definido
CREATE TABLE IF NOT EXISTS events.processed_events_default
    PARTITION OF events.processed_events DEFAULT;

-- Índices para consultas frequentes
CREATE INDEX IF NOT EXISTS idx_processed_event_id
    ON events.processed_events (event_id);

CREATE INDEX IF NOT EXISTS idx_processed_source
    ON events.processed_events (source);

CREATE INDEX IF NOT EXISTS idx_processed_event_type
    ON events.processed_events (event_type);

CREATE INDEX IF NOT EXISTS idx_processed_status
    ON events.processed_events (status);

CREATE INDEX IF NOT EXISTS idx_processed_correlation_id
    ON events.processed_events (correlation_id);

CREATE INDEX IF NOT EXISTS idx_processed_payload_gin
    ON events.processed_events USING GIN (payload jsonb_path_ops);

-- ============================================================
-- Tabela de alertas gerados
-- ============================================================
CREATE TABLE IF NOT EXISTS events.alerts (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    alert_id        UUID NOT NULL UNIQUE,
    rule_name       VARCHAR(255) NOT NULL,
    description     TEXT,
    source_event_id UUID NOT NULL,
    event_type      VARCHAR(255),
    priority        VARCHAR(20) NOT NULL DEFAULT 'HIGH',
    triggered_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    acknowledged    BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    acknowledged_by VARCHAR(255),
    metadata        JSONB,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_alerts_rule_name
    ON events.alerts (rule_name);

CREATE INDEX IF NOT EXISTS idx_alerts_triggered_at
    ON events.alerts (triggered_at);

CREATE INDEX IF NOT EXISTS idx_alerts_acknowledged
    ON events.alerts (acknowledged) WHERE NOT acknowledged;

-- ============================================================
-- Tabela de Dead-Letter Queue (para auditoria e reprocessamento)
-- ============================================================
CREATE TABLE IF NOT EXISTS events.dead_letter_events (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dlq_id          UUID NOT NULL UNIQUE,
    original_event  JSONB NOT NULL,
    origin_topic    VARCHAR(100) NOT NULL,
    error_message   TEXT NOT NULL,
    stack_trace     TEXT,
    retry_count     INTEGER NOT NULL DEFAULT 0,
    resolved        BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at     TIMESTAMP WITH TIME ZONE,
    failed_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dlq_origin_topic
    ON events.dead_letter_events (origin_topic);

CREATE INDEX IF NOT EXISTS idx_dlq_resolved
    ON events.dead_letter_events (resolved) WHERE NOT resolved;

CREATE INDEX IF NOT EXISTS idx_dlq_failed_at
    ON events.dead_letter_events (failed_at);

-- ============================================================
-- Grants (usuário da aplicação)
-- ============================================================
GRANT USAGE ON SCHEMA events TO streaming;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA events TO streaming;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA events TO streaming;
ALTER DEFAULT PRIVILEGES IN SCHEMA events GRANT ALL ON TABLES TO streaming;
ALTER DEFAULT PRIVILEGES IN SCHEMA events GRANT ALL ON SEQUENCES TO streaming;