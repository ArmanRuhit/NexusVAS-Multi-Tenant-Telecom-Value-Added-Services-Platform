-- Auth Service: Roles Table

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(36),              -- NULL for platform-level roles
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    is_system_role BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, name)
);

CREATE INDEX idx_roles_tenant_id ON roles(tenant_id);

-- Insert system roles
INSERT INTO roles (id, tenant_id, name, description, is_system_role) VALUES
    (gen_random_uuid(), NULL, 'PLATFORM_ADMIN', 'Full platform administrator', TRUE),
    (gen_random_uuid(), NULL, 'OPERATOR_ADMIN', 'Operator administrator', TRUE),
    (gen_random_uuid(), NULL, 'OPERATOR_USER', 'Operator staff user', TRUE),
    (gen_random_uuid(), NULL, 'SUBSCRIBER', 'End subscriber', TRUE);
