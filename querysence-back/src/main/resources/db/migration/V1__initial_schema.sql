-- Flyway Migration: V1__initial_schema.sql
-- Initial database schema for QuerySence

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Projects table
CREATE TABLE IF NOT EXISTS projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(owner_id, name)
);

-- Schema definitions table
CREATE TABLE IF NOT EXISTS schema_definition (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    dialect VARCHAR(50) NOT NULL,
    project_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Table definitions table
CREATE TABLE IF NOT EXISTS table_definition (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    estimated_rows BIGINT DEFAULT 0,
    description TEXT,
    schema_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (schema_id) REFERENCES schema_definition(id) ON DELETE CASCADE
);

-- Column definitions table
CREATE TABLE IF NOT EXISTS column_definition (
    id BIGSERIAL PRIMARY KEY,
    column_name VARCHAR(100) NOT NULL,
    data_type VARCHAR(100) NOT NULL,
    is_nullable BOOLEAN DEFAULT true,
    table_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (table_id) REFERENCES table_definition(id) ON DELETE CASCADE
);

-- Index definitions table
CREATE TABLE IF NOT EXISTS index_definition (
    id BIGSERIAL PRIMARY KEY,
    index_name VARCHAR(100) NOT NULL,
    column_names TEXT NOT NULL,
    table_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (table_id) REFERENCES table_definition(id) ON DELETE CASCADE
);

-- Query history table
CREATE TABLE IF NOT EXISTS query_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    project_id BIGINT,
    query_text TEXT NOT NULL,
    query_hash VARCHAR(64),
    execution_time_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL
);

-- Create indexes on query_history
CREATE INDEX IF NOT EXISTS idx_query_history_user ON query_history(user_id);
CREATE INDEX IF NOT EXISTS idx_query_history_project ON query_history(project_id);
CREATE INDEX IF NOT EXISTS idx_query_history_hash ON query_history(query_hash);

-- AI usage logs table
CREATE TABLE IF NOT EXISTS ai_usage_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    feature VARCHAR(50),
    tokens_used INT,
    cost_usd DECIMAL(10, 4),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Index suggestions table
CREATE TABLE IF NOT EXISTS index_suggestion (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    column_names TEXT NOT NULL,
    impact_score DECIMAL(5, 2),
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Security findings table
CREATE TABLE IF NOT EXISTS security_finding (
    id BIGSERIAL PRIMARY KEY,
    severity VARCHAR(20),
    finding_type VARCHAR(100),
    description TEXT,
    recommendation TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Optimization logs table
CREATE TABLE IF NOT EXISTS optimization_log (
    id BIGSERIAL PRIMARY KEY,
    query_text TEXT,
    optimized_query TEXT,
    improvement_percentage DECIMAL(5, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
