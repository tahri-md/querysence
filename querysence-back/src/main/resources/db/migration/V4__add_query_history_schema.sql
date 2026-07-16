-- V4__add_query_history_schema.sql

ALTER TABLE query_history
    ADD COLUMN IF NOT EXISTS schema_id BIGINT REFERENCES schema_definitions(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_query_history_schema ON query_history(schema_id);
