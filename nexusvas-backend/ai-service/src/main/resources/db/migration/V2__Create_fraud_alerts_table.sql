-- Create fraud_alerts table for AI Service fraud detection
CREATE TABLE IF NOT EXISTS fraud_alerts (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    msisdn VARCHAR(20) NOT NULL,
    fraud_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description TEXT,
    raw_event TEXT,
    resolution TEXT,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP WITH TIME ZONE,
    acknowledged_by VARCHAR(100),
    resolved_by VARCHAR(100)
);

-- Create indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_fraud_tenant_status ON fraud_alerts(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_fraud_tenant_msisdn ON fraud_alerts(tenant_id, msisdn);
CREATE INDEX IF NOT EXISTS idx_fraud_detected_at ON fraud_alerts(detected_at DESC);
CREATE INDEX IF NOT EXISTS idx_fraud_severity ON fraud_alerts(severity);

-- Comment on table
COMMENT ON TABLE fraud_alerts IS 'Stores fraud detection alerts from AI Service';
COMMENT ON COLUMN fraud_alerts.fraud_type IS 'Type: UNUSUAL_AMOUNT, RAPID_CHARGES, SUSPICIOUS_REFUND, ACCOUNT_TAKEOVER, SUBSCRIPTION_ABUSE, VELOCITY_BREACH, GEO_ANOMALY';
COMMENT ON COLUMN fraud_alerts.severity IS 'Severity level: LOW, MEDIUM, HIGH, CRITICAL';
COMMENT ON COLUMN fraud_alerts.status IS 'Alert status: ACTIVE, ACKNOWLEDGED, RESOLVED, FALSE_POSITIVE';
