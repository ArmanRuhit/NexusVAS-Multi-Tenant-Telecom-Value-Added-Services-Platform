-- AI Service: Prompt Logs (for auditing and improvement)

CREATE TABLE prompt_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(36),
    user_id UUID,
    prompt_type VARCHAR(30) NOT NULL CHECK (prompt_type IN ('CHAT', 'RAG_QUERY', 'SUMMARY', 'ANALYSIS')),
    prompt_text TEXT NOT NULL,
    response_text TEXT,
    model_name VARCHAR(50) NOT NULL,
    tokens_used INTEGER,
    latency_ms INTEGER,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'TIMEOUT')),
    error_message VARCHAR(500),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_prompt_logs_tenant_id ON prompt_logs(tenant_id);
CREATE INDEX idx_prompt_logs_prompt_type ON prompt_logs(prompt_type);
CREATE INDEX idx_prompt_logs_created_at ON prompt_logs(created_at);
