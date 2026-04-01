-- Operator Service: Operators (Tenants) Table

CREATE TABLE operators (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(50) NOT NULL UNIQUE,           -- URL-friendly identifier
    country VARCHAR(2) NOT NULL,                 -- ISO 3166-1 alpha-2
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',  -- ISO 4217
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'TERMINATED')),
    billing_model VARCHAR(20) NOT NULL CHECK (billing_model IN ('PREPAID', 'POSTPAID', 'HYBRID')),
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(20),
    address JSONB,
    contract_start_date DATE,
    contract_end_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_operators_status ON operators(status);
CREATE INDEX idx_operators_country ON operators(country);
