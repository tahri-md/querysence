-- V3__add_db_connections.sql
-- Adds live database connection support: db_connections, schema_sync_logs, execution_plans,
-- plus new columns on schema_definitions and query_history.

CREATE TABLE db_connections (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name                VARCHAR(100) NOT NULL,
    host                VARCHAR(255) NOT NULL,
    port                INTEGER NOT NULL,
    database_name       VARCHAR(100) NOT NULL,
    username            VARCHAR(100) NOT NULL,
    encrypted_password  TEXT NOT NULL,
    dialect             VARCHAR(20) NOT NULL DEFAULT 'POSTGRESQL',
    ssl_enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    read_only_enforced  BOOLEAN NOT NULL DEFAULT TRUE,
    status              VARCHAR(20) NOT NULL DEFAULT 'UNTESTED',
    last_tested_at      TIMESTAMP,
    last_synced_at      TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_db_connection_name_per_project UNIQUE (project_id, name)
);

CREATE INDEX idx_db_connections_project ON db_connections(project_id);

CREATE TABLE schema_sync_logs (
    id                  BIGSERIAL PRIMARY KEY,
    db_connection_id    BIGINT NOT NULL REFERENCES db_connections(id) ON DELETE CASCADE,
    status              VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    tables_discovered   INTEGER,
    columns_discovered  INTEGER,
    indexes_discovered  INTEGER,
    error_message       TEXT,
    started_at          TIMESTAMP NOT NULL DEFAULT now(),
    finished_at         TIMESTAMP
);

CREATE INDEX idx_schema_sync_logs_connection ON schema_sync_logs(db_connection_id);

-- schema_definitions: track manual vs synced, and which connection populated it
ALTER TABLE schema_definitions
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN db_connection_id BIGINT REFERENCES db_connections(id) ON DELETE SET NULL;

CREATE INDEX idx_schema_definitions_db_connection ON schema_definitions(db_connection_id);

-- query_history: track which live connection (if any) a query was analyzed against
ALTER TABLE query_history
    ADD COLUMN db_connection_id BIGINT REFERENCES db_connections(id) ON DELETE SET NULL;

CREATE INDEX idx_query_history_db_connection ON query_history(db_connection_id);

CREATE TABLE execution_plans (
    id                  BIGSERIAL PRIMARY KEY,
    query_history_id    BIGINT NOT NULL UNIQUE REFERENCES query_history(id) ON DELETE CASCADE,
    db_connection_id    BIGINT REFERENCES db_connections(id) ON DELETE SET NULL,
    source              VARCHAR(20) NOT NULL DEFAULT 'STATIC_HEURISTIC',
    plan_text           TEXT,
    plan_json           TEXT,
    estimated_cost      DOUBLE PRECISION,
    actual_rows         BIGINT,
    actual_time_ms      DOUBLE PRECISION,
    used_indexes        TEXT[],
    full_table_scans    TEXT[],
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_execution_plans_db_connection ON execution_plans(db_connection_id);
