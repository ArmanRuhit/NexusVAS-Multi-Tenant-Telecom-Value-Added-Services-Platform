-- Billing Service: Billing Accounts Table

CREATE TABLE billing_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(36) NOT NULL,
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('OPERATOR', 'SUBSCRIBER', 'SYSTEM')),
    reference_id VARCHAR(100) NOT NULL,         -- operator_id or msisdn
    balance DECIMAL(12, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    credit_limit DECIMAL(12, 2) DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, reference_id)
);

CREATE INDEX idx_billing_accounts_tenant_id ON billing_accounts(tenant_id);
CREATE INDEX idx_billing_accounts_reference_id ON billing_accounts(reference_id);
