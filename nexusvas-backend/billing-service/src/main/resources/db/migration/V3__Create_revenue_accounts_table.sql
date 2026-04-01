-- Billing Service: Revenue Accounts (for operator revenue tracking)

CREATE TABLE revenue_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(36) NOT NULL,
    account_code VARCHAR(20) NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    account_category VARCHAR(30) NOT NULL CHECK (account_category IN ('REVENUE', 'TAX', 'COMMISSION', 'DISCOUNT')),
    balance DECIMAL(12, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, account_code)
);

CREATE INDEX idx_revenue_accounts_tenant_id ON revenue_accounts(tenant_id);
