-- Auth Service: Role-Permission Association Table

CREATE TABLE role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    UNIQUE(role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

-- Assign permissions to PLATFORM_ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'PLATFORM_ADMIN';

-- Assign permissions to OPERATOR_ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'OPERATOR_ADMIN' 
AND p.name IN ('USER_CREATE', 'USER_READ', 'USER_UPDATE', 'USER_DELETE',
               'PRODUCT_CREATE', 'PRODUCT_READ', 'PRODUCT_UPDATE', 'PRODUCT_DELETE',
               'CAMPAIGN_CREATE', 'CAMPAIGN_READ', 'CAMPAIGN_UPDATE', 'CAMPAIGN_DELETE',
               'BILLING_READ', 'BILLING_MANAGE', 'ANALYTICS_READ');

-- Assign permissions to OPERATOR_USER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'OPERATOR_USER'
AND p.name IN ('USER_READ', 'PRODUCT_READ', 'CAMPAIGN_READ', 'BILLING_READ', 'ANALYTICS_READ');

-- Assign permissions to SUBSCRIBER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'SUBSCRIBER'
AND p.name IN ('SUBSCRIPTION_READ', 'SUBSCRIPTION_UPDATE');
