-- Create operator_api_keys table for API key management
CREATE TABLE IF NOT EXISTS operator_api_keys (
    id UUID PRIMARY KEY,
    operator_id UUID NOT NULL REFERENCES operators(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    key_hash TEXT NOT NULL,
    prefix VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_api_key_operator ON operator_api_keys(operator_id);
CREATE INDEX IF NOT EXISTS idx_api_key_prefix ON operator_api_keys(prefix);

-- Comment
COMMENT ON TABLE operator_api_keys IS 'Stores API keys for operator authentication';
