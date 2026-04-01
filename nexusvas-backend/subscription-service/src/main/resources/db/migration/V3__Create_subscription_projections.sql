-- Subscription Service: Subscription Projections (Read Model)

CREATE TABLE subscription_projections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL UNIQUE,
    tenant_id VARCHAR(36) NOT NULL,
    msisdn VARCHAR(20) NOT NULL,
    product_id UUID NOT NULL,
    product_name VARCHAR(100),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'PENDING', 'CANCELLED', 'EXPIRED', 'SUSPENDED')),
    billing_cycle VARCHAR(20) NOT NULL CHECK (billing_cycle IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    price_amount DECIMAL(10, 2) NOT NULL,
    price_currency VARCHAR(3) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason VARCHAR(255),
    renewal_count INTEGER DEFAULT 0,
    last_billed_at TIMESTAMP WITH TIME ZONE,
    next_billing_at TIMESTAMP WITH TIME ZONE,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subscription_projections_tenant_id ON subscription_projections(tenant_id);
CREATE INDEX idx_subscription_projections_msisdn ON subscription_projections(msisdn);
CREATE INDEX idx_subscription_projections_status ON subscription_projections(status);
CREATE INDEX idx_subscription_projections_expires_at ON subscription_projections(expires_at);
CREATE INDEX idx_subscription_projections_next_billing ON subscription_projections(next_billing_at);
