-- AI Service: Vector Embeddings Table (for RAG)

-- Enable pgvector extension (should already be enabled by init script)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE vector_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(36),
    content_type VARCHAR(30) NOT NULL CHECK (content_type IN ('FAQ', 'DOCUMENT', 'PRODUCT', 'POLICY', 'KNOWLEDGE_BASE')),
    content_id VARCHAR(100) NOT NULL,
    content_text TEXT NOT NULL,
    embedding vector(1536),                     -- OpenAI embeddings dimension
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(content_type, content_id)
);

-- Create vector similarity search index
CREATE INDEX idx_vector_embeddings_embedding ON vector_embeddings 
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE INDEX idx_vector_embeddings_tenant_id ON vector_embeddings(tenant_id);
CREATE INDEX idx_vector_embeddings_content_type ON vector_embeddings(content_type);
