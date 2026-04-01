package dev.armanruhit.nexusvas.subscription.exception;

import lombok.Getter;

@Getter
public class SubscriptionException extends RuntimeException {

    private final String errorCode;

    public SubscriptionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
