-- Campaign delivery results table: tracks individual message delivery per subscriber
CREATE TABLE campaign_delivery_results (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id     UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    batch_id        UUID REFERENCES campaign_batches(id) ON DELETE SET NULL,
    msisdn          VARCHAR(20) NOT NULL,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at         TIMESTAMPTZ,
    delivered_at    TIMESTAMPTZ,
    clicked_at      TIMESTAMPTZ,
    converted_at    TIMESTAMPTZ,
    error_code      VARCHAR(50),
    error_message   VARCHAR(500),
    provider_message_id VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT ck_delivery_status CHECK (delivery_status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'CLICKED', 'CONVERTED'))
);

-- Indexes for common query patterns
CREATE INDEX idx_delivery_campaign ON campaign_delivery_results(campaign_id);
CREATE INDEX idx_delivery_batch ON campaign_delivery_results(batch_id);
CREATE INDEX idx_delivery_msisdn ON campaign_delivery_results(msisdn);
CREATE INDEX idx_delivery_status ON campaign_delivery_results(delivery_status);
CREATE INDEX idx_delivery_created ON campaign_delivery_results(created_at);

-- Composite index for campaign performance queries
CREATE INDEX idx_delivery_campaign_status ON campaign_delivery_results(campaign_id, delivery_status);
