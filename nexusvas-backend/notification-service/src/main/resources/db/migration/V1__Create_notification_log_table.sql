-- Notification Service: Notification Log Table

CREATE TABLE notification_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(36) NOT NULL,
    correlation_id UUID NOT NULL,
    notification_type VARCHAR(20) NOT NULL CHECK (notification_type IN ('SMS', 'PUSH', 'IN_APP', 'EMAIL')),
    msisdn VARCHAR(20),
    user_id UUID,
    subject VARCHAR(200),
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'BOUNCED')),
    provider VARCHAR(50),
    provider_message_id VARCHAR(100),
    error_code VARCHAR(50),
    error_message VARCHAR(500),
    retry_count INTEGER DEFAULT 0,
    sent_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_log_tenant_id ON notification_log(tenant_id);
CREATE INDEX idx_notification_log_correlation_id ON notification_log(correlation_id);
CREATE INDEX idx_notification_log_msisdn ON notification_log(msisdn);
CREATE INDEX idx_notification_log_status ON notification_log(status);
CREATE INDEX idx_notification_log_created_at ON notification_log(created_at);
