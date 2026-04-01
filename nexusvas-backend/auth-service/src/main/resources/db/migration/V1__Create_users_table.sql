-- Auth Service: Users Table
-- Stores all users: platform admins, operator users, and subscribers

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(36),              -- NULL for platform admins
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    msisdn VARCHAR(20),                 -- for subscribers
    user_type VARCHAR(20) NOT NULL CHECK (user_type IN ('PLATFORM_ADMIN', 'OPERATOR_USER', 'SUBSCRIBER')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'LOCKED', 'SUSPENDED', 'DELETED')),
    mfa_enabled BOOLEAN DEFAULT FALSE,
    mfa_secret VARCHAR(255),
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_msisdn ON users(msisdn);
CREATE UNIQUE INDEX idx_users_tenant_email ON users(tenant_id, email) WHERE tenant_id IS NOT NULL;
CREATE UNIQUE INDEX idx_users_tenant_msisdn ON users(tenant_id, msisdn) WHERE msisdn IS NOT NULL;

COMMENT ON TABLE users IS 'Stores all users: platform admins, operator users, and subscribers';
COMMENT ON COLUMN users.tenant_id IS 'References operator_id for operator users and subscribers, NULL for platform admins';
