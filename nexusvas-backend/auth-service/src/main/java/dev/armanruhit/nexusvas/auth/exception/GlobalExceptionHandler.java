package dev.armanruhit.nexusvas.auth.exception;

import dev.armanruhit.nexusvas.common_lib.dto.ApiResponse;
import dev.armanruhit.nexusvas.common_lib.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case "INVALID_CREDENTIALS", "INVALID_API_KEY", "INVALID_OTP",
                 "INVALID_REFRESH_TOKEN", "INVALID_TOTP", "INVALID_CHALLENGE" ->
                HttpStatus.UNAUTHORIZED;
            case "ACCOUNT_LOCKED", "ACCOUNT_INACTIVE" -> HttpStatus.FORBIDDEN;
            case "USER_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };

        log.warn("Auth error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(status)
            .body(ApiResponse.error(ErrorResponse.of(ex.getErrorCode(), ex.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<dev.armanruhit.nexusvas.common_lib.dto.FieldError> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fe -> new dev.armanruhit.nexusvas.common_lib.dto.FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();

        ErrorResponse errorResponse = ErrorResponse.validation(errors);
        return ResponseEntity.badRequest().body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
            .body(ApiResponse.error(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred")));
    }
}
