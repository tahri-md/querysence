ALTER TABLE column_definitions
    ADD COLUMN distinct_count DOUBLE PRECISION,
    ADD COLUMN null_fraction DOUBLE PRECISION,
    ADD COLUMN stats_updated_at TIMESTAMP;