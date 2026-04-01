-- Auth Service: Additional performance indexes
-- V11: Query-path indexes not covered by earlier migrations

-- Users
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_tenant_status ON users(tenant_id, status) WHERE tenant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_locked_until ON users(locked_until) WHERE locked_until IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_last_login ON users(last_login_at DESC NULLS LAST);

-- API Keys
CREATE INDEX IF NOT EXISTS idx_api_keys_tenant_status ON api_keys(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_api_keys_expires_at ON api_keys(expires_at) WHERE status = 'ACTIVE';

-- Audit Log
CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_created ON audit_log(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_event_status ON audit_log(event_type, status);

-- Roles
CREATE INDEX IF NOT EXISTS idx_roles_system ON roles(is_system_role) WHERE is_system_role = true;
CREATE INDEX IF NOT EXISTS idx_roles_name ON roles(name);
