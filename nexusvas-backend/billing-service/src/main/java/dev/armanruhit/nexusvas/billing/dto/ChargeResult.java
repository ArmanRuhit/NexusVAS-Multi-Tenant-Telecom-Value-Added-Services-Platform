package dev.armanruhit.nexusvas.billing.dto;

import java.util.UUID;

public record ChargeResult(
    String status,  // SUCCESS, FAILED, DUPLICATE
    UUID transactionId,
    String failureReason
) {
    public static ChargeResult success(UUID transactionId) {
        return new ChargeResult("SUCCESS", transactionId, null);
    }

    public static ChargeResult failed(String reason) {
        return new ChargeResult("FAILED", null, reason);
    }

    public static ChargeResult duplicate(UUID transactionId) {
        return new ChargeResult("DUPLICATE", transactionId, null);
    }

    public boolean isSuccess() { return "SUCCESS".equals(status) || "DUPLICATE".equals(status); }
}
