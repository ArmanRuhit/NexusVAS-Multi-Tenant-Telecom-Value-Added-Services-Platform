-- Auth Service: Seed system roles and permissions
-- V10: Default roles and permission catalog

-- ── Permissions ───────────────────────────────────────────────────────────────

INSERT INTO permissions (id, name, resource, action, description) VALUES
    (gen_random_uuid(), 'subscription:read',   'subscription', 'read',   'View subscriptions'),
    (gen_random_uuid(), 'subscription:write',  'subscription', 'write',  'Create and modify subscriptions'),
    (gen_random_uuid(), 'billing:read',        'billing',      'read',   'View billing records'),
    (gen_random_uuid(), 'billing:write',       'billing',      'write',  'Execute billing operations'),
    (gen_random_uuid(), 'campaign:read',       'campaign',     'read',   'View campaigns'),
    (gen_random_uuid(), 'campaign:create',     'campaign',     'create', 'Create and launch campaigns'),
    (gen_random_uuid(), 'content:read',        'content',      'read',   'View content catalog'),
    (gen_random_uuid(), 'content:manage',      'content',      'manage', 'Create and manage content'),
    (gen_random_uuid(), 'analytics:view',      'analytics',    'view',   'View analytics dashboards'),
    (gen_random_uuid(), 'ai:invoke',           'ai',           'invoke', 'Invoke AI features'),
    (gen_random_uuid(), 'operator:manage',     'operator',     'manage', 'Manage operator configuration'),
    (gen_random_uuid(), 'user:manage',         'user',         'manage', 'Manage users and roles'),
    (gen_random_uuid(), 'notification:send',   'notification', 'send',   'Send notifications')
ON CONFLICT (name) DO NOTHING;

-- ── System Roles ──────────────────────────────────────────────────────────────

INSERT INTO roles (id, name, tenant_id, description, is_system) VALUES
    (gen_random_uuid(), 'SUPER_ADMIN',      NULL, 'Full platform access across all tenants', true),
    (gen_random_uuid(), 'OPERATOR_ADMIN',   NULL, 'Full access within an operator tenant',   true),
    (gen_random_uuid(), 'OPERATOR_VIEWER',  NULL, 'Read-only access within an operator tenant', true),
    (gen_random_uuid(), 'SUBSCRIBER',       NULL, 'Subscriber self-service access',           true)
ON CONFLICT DO NOTHING;

-- ── Role → Permission Assignments ─────────────────────────────────────────────

-- SUPER_ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN' AND r.is_system = true
ON CONFLICT DO NOTHING;

-- OPERATOR_ADMIN gets everything except user:manage and operator:manage at platform level
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'OPERATOR_ADMIN' AND r.is_system = true
  AND p.name IN (
      'subscription:read', 'subscription:write',
      'billing:read', 'billing:write',
      'campaign:read', 'campaign:create',
      'content:read', 'content:manage',
      'analytics:view', 'ai:invoke',
      'notification:send'
  )
ON CONFLICT DO NOTHING;

-- OPERATOR_VIEWER gets read-only permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'OPERATOR_VIEWER' AND r.is_system = true
  AND p.name IN (
      'subscription:read',
      'billing:read',
      'campaign:read',
      'content:read',
      'analytics:view'
  )
ON CONFLICT DO NOTHING;

-- SUBSCRIBER gets self-service only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SUBSCRIBER' AND r.is_system = true
  AND p.name IN ('subscription:read', 'content:read')
ON CONFLICT DO NOTHING;
