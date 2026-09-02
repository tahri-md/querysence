CREATE TABLE query_examples (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    schema_id BIGINT NOT NULL REFERENCES schema_definitions(id),
    nl_query TEXT NOT NULL,
    sql_output TEXT NOT NULL,
    query_type VARCHAR(50),
    confidence_score DECIMAL(3,2),
    token_count INTEGER,
    execution_time_ms BIGINT,
    verified BOOLEAN DEFAULT FALSE,
    pinecone_vector_id VARCHAR(255) UNIQUE,
    access_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_query_examples_schema_id ON query_examples(schema_id);
CREATE INDEX idx_query_examples_user_id ON query_examples(user_id);
CREATE INDEX idx_query_examples_verified ON query_examples(verified);
CREATE INDEX idx_query_examples_confidence ON query_examples(confidence_score);
CREATE INDEX idx_query_examples_created_at ON query_examples(created_at);