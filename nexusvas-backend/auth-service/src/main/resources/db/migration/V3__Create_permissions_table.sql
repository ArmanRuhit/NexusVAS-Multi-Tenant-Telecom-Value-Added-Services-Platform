-- Auth Service: Permissions Table

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('CREATE', 'READ', 'UPDATE', 'DELETE', 'MANAGE')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert base permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    -- Platform Admin permissions
    ('PLATFORM_MANAGE', 'Manage entire platform', 'PLATFORM', 'MANAGE'),
    ('OPERATOR_CREATE', 'Create new operators', 'OPERATOR', 'CREATE'),
    ('OPERATOR_READ', 'View operators', 'OPERATOR', 'READ'),
    ('OPERATOR_UPDATE', 'Update operators', 'OPERATOR', 'UPDATE'),
    ('OPERATOR_DELETE', 'Delete operators', 'OPERATOR', 'DELETE'),
    
    -- Operator Admin permissions
    ('USER_CREATE', 'Create users', 'USER', 'CREATE'),
    ('USER_READ', 'View users', 'USER', 'READ'),
    ('USER_UPDATE', 'Update users', 'USER', 'UPDATE'),
    ('USER_DELETE', 'Delete users', 'USER', 'DELETE'),
    
    ('PRODUCT_CREATE', 'Create products', 'PRODUCT', 'CREATE'),
    ('PRODUCT_READ', 'View products', 'PRODUCT', 'READ'),
    ('PRODUCT_UPDATE', 'Update products', 'PRODUCT', 'UPDATE'),
    ('PRODUCT_DELETE', 'Delete products', 'PRODUCT', 'DELETE'),
    
    ('CAMPAIGN_CREATE', 'Create campaigns', 'CAMPAIGN', 'CREATE'),
    ('CAMPAIGN_READ', 'View campaigns', 'CAMPAIGN', 'READ'),
    ('CAMPAIGN_UPDATE', 'Update campaigns', 'CAMPAIGN', 'UPDATE'),
    ('CAMPAIGN_DELETE', 'Delete campaigns', 'CAMPAIGN', 'DELETE'),
    
    ('BILLING_READ', 'View billing', 'BILLING', 'READ'),
    ('BILLING_MANAGE', 'Manage billing', 'BILLING', 'MANAGE'),
    
    ('ANALYTICS_READ', 'View analytics', 'ANALYTICS', 'READ'),
    
    -- Subscriber permissions
    ('SUBSCRIPTION_READ', 'View own subscription', 'SUBSCRIPTION', 'READ'),
    ('SUBSCRIPTION_UPDATE', 'Update own subscription', 'SUBSCRIPTION', 'UPDATE');
