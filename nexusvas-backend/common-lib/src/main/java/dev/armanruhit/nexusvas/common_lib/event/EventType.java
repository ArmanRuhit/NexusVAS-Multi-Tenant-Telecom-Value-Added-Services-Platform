package dev.armanruhit.nexusvas.common_lib.event;

public final class EventType {
    private EventType() {}
    
    // Operator Domain
    public static final String OPERATOR_ONBOARDED = "OPERATOR_ONBOARDED";
    public static final String OPERATOR_SUSPENDED = "OPERATOR_SUSPENDED";
    public static final String API_KEY_ROTATED = "API_KEY_ROTATED";
    
    // Auth Domain
    public static final String USER_REGISTERED = "USER_REGISTERED";
    public static final String USER_LOCKED = "USER_LOCKED";
    public static final String ROLE_ASSIGNED = "ROLE_ASSIGNED";
    public static final String ROLE_REVOKED = "ROLE_REVOKED";
    public static final String TOKEN_REVOKED = "TOKEN_REVOKED";
    public static final String API_KEY_CREATED = "API_KEY_CREATED";
    public static final String API_KEY_REVOKED = "API_KEY_REVOKED";
    public static final String PERMISSION_DENIED = "PERMISSION_DENIED";
    
    // Subscription Domain
    public static final String SUBSCRIPTION_CREATED = "SUBSCRIPTION_CREATED";
    public static final String SUBSCRIPTION_RENEWED = "SUBSCRIPTION_RENEWED";
    public static final String SUBSCRIPTION_CANCELLED = "SUBSCRIPTION_CANCELLED";
    public static final String SUBSCRIPTION_EXPIRED = "SUBSCRIPTION_EXPIRED";
    
    // Billing Domain
    public static final String CHARGE_INITIATED = "CHARGE_INITIATED";
    public static final String CHARGE_SUCCEEDED = "CHARGE_SUCCEEDED";
    public static final String CHARGE_FAILED = "CHARGE_FAILED";
    public static final String REFUND_ISSUED = "REFUND_ISSUED";
    
    // Content Domain
    public static final String CONTENT_PUBLISHED = "CONTENT_PUBLISHED";
    public static final String CONTENT_DELIVERED = "CONTENT_DELIVERED";
    public static final String CONTENT_EXPIRED = "CONTENT_EXPIRED";
    
    // Campaign Domain
    public static final String CAMPAIGN_LAUNCHED = "CAMPAIGN_LAUNCHED";
    public static final String CAMPAIGN_COMPLETED = "CAMPAIGN_COMPLETED";
    public static final String CAMPAIGN_FAILED = "CAMPAIGN_FAILED";
    
    // AI Domain
    public static final String CHURN_RISK_SCORED = "CHURN_RISK_SCORED";
    public static final String RECOMMENDATION_GENERATED = "RECOMMENDATION_GENERATED";
    public static final String FRAUD_FLAGGED = "FRAUD_FLAGGED";
    public static final String CAMPAIGN_COPY_GENERATED = "CAMPAIGN_COPY_GENERATED";
}