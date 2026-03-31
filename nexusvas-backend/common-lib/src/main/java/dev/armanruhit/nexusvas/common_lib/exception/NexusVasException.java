package dev.armanruhit.nexusvas.common_lib.exception;

import lombok.Getter;

@Getter
public class NexusVasException extends RuntimeException{
    private final String errorCode;

    public NexusVasException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public NexusVasException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
