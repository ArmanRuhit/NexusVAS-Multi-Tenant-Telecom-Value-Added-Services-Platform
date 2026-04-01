-- Subscription Service: Additional indexes
-- V4: Composite indexes for common query patterns

-- Outbox: speed up the poller query
CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON outbox(status, created_at ASC)
    WHERE status IN ('PENDING', 'FAILED');
CREATE INDEX IF NOT EXISTS idx_outbox_retry_count ON outbox(retry_count) WHERE status = 'FAILED';

-- Event store: tenant-scoped aggregate lookups
CREATE INDEX IF NOT EXISTS idx_event_store_tenant_aggregate ON event_store(tenant_id, aggregate_id, version ASC);

-- Subscription projections: additional query patterns
CREATE INDEX IF NOT EXISTS idx_sub_proj_tenant_msisdn ON subscription_projections(tenant_id, msisdn);
CREATE INDEX IF NOT EXISTS idx_sub_proj_tenant_product ON subscription_projections(tenant_id, product_id, status);
CREATE INDEX IF NOT EXISTS idx_sub_proj_active_billing ON subscription_projections(next_billing_at ASC)
    WHERE status = 'ACTIVE';
