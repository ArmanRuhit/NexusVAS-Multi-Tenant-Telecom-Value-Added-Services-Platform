-- Auth Service: Add cross-service VAS permissions
-- V10: Extend permission catalog for microservice-level access control

INSERT INTO permissions (name, description, resource, action) VALUES
    ('SUBSCRIPTION_CREATE', 'Create subscriptions',         'SUBSCRIPTION', 'CREATE'),
    ('SUBSCRIPTION_DELETE', 'Cancel subscriptions',         'SUBSCRIPTION', 'DELETE'),
    ('BILLING_CREATE',      'Initiate billing charges',     'BILLING',      'CREATE'),
    ('BILLING_DELETE',      'Issue refunds',                'BILLING',      'DELETE'),
    ('CONTENT_CREATE',      'Create content items',         'CONTENT',      'CREATE'),
    ('CONTENT_READ',        'View content catalog',         'CONTENT',      'READ'),
    ('CONTENT_UPDATE',      'Update content metadata',      'CONTENT',      'UPDATE'),
    ('CONTENT_DELETE',      'Archive content items',        'CONTENT',      'DELETE'),
    ('NOTIFICATION_CREATE', 'Send notifications',           'NOTIFICATION', 'CREATE'),
    ('AI_READ',             'View AI-generated insights',   'AI',           'READ'),
    ('AI_CREATE',           'Invoke AI features',           'AI',           'CREATE')
ON CONFLICT (name) DO NOTHING;

-- Grant content and notification permissions to OPERATOR_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'OPERATOR_ADMIN'
  AND p.name IN (
      'SUBSCRIPTION_CREATE', 'SUBSCRIPTION_DELETE',
      'BILLING_CREATE', 'BILLING_DELETE',
      'CONTENT_CREATE', 'CONTENT_READ', 'CONTENT_UPDATE', 'CONTENT_DELETE',
      'NOTIFICATION_CREATE', 'AI_READ', 'AI_CREATE'
  )
ON CONFLICT DO NOTHING;

-- Grant read permissions to OPERATOR_USER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'OPERATOR_USER'
  AND p.name IN ('CONTENT_READ', 'AI_READ', 'SUBSCRIPTION_READ')
ON CONFLICT DO NOTHING;

-- Grant subscriber permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'SUBSCRIBER'
  AND p.name IN ('CONTENT_READ')
ON CONFLICT DO NOTHING;
