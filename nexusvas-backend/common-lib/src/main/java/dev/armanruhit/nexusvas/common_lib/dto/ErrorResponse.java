package dev.armanruhit.nexusvas.common_lib.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ErrorResponse(
    String code,
    String message,
    String traceId,
    Instant timestamp,
    List<FieldError> details
) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, UUID.randomUUID().toString(), Instant.now(), null);
    }

    public static ErrorResponse validation(List<FieldError> details) {
        return new ErrorResponse("VALIDATION_ERROR", "Request validation failed",
                UUID.randomUUID().toString(), Instant.now(), details);
    }
}
