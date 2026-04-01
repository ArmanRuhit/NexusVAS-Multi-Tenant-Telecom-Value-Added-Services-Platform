package dev.armanruhit.nexusvas.billing.exception;

import lombok.Getter;

@Getter
public class BillingException extends RuntimeException {

    private final String errorCode;

    public BillingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
