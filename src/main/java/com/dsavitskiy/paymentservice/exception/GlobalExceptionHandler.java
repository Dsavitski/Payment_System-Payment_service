package com.dsavitskiy.paymentservice.exception;

import com.dsavitskiy.paymentservice.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ForbiddenAccessException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenAccessException(ForbiddenAccessException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Access denied",
                ex.getMessage()
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Access denied",
                ex.getMessage()
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ErrorResponse> handleExternalApiException(ExternalApiException ex) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "External API is unavailable",
                ex.getMessage()
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Validation Error",
                errorMessage
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Authentication failed",
                ex.getMessage()
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Internal server error",
                ex.getMessage()
        );

        return ResponseEntity.status(status).body(error);
    }
}