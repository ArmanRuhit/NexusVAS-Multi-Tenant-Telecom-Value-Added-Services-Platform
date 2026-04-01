-- AI Service: Churn Prediction Scores

CREATE TABLE churn_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(36) NOT NULL,
    msisdn VARCHAR(20) NOT NULL,
    score DECIMAL(5, 4) NOT NULL CHECK (score >= 0 AND score <= 1),
    risk_level VARCHAR(20) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    contributing_factors JSONB,
    model_version VARCHAR(20) NOT NULL,
    prediction_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, msisdn, prediction_date)
);

CREATE INDEX idx_churn_scores_tenant_id ON churn_scores(tenant_id);
CREATE INDEX idx_churn_scores_msisdn ON churn_scores(msisdn);
CREATE INDEX idx_churn_scores_risk_level ON churn_scores(risk_level);
CREATE INDEX idx_churn_scores_prediction_date ON churn_scores(prediction_date);
