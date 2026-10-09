package com.ridelink.ride_management_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - Ride not found
    @ExceptionHandler(RideNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRideNotFound(
            RideNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "RIDE_NOT_FOUND", ex.getMessage(), request.getRequestURI());
    }

    // 409 - Invalid state transition
    @ExceptionHandler(InvalidRideTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransition(
            InvalidRideTransitionException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "INVALID_RIDE_TRANSITION", ex.getMessage(), request.getRequestURI());
    }

    // 409 - No available driver
    @ExceptionHandler(NoAvailableDriverException.class)
    public ResponseEntity<Map<String, Object>> handleNoDriver(
            NoAvailableDriverException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "NO_AVAILABLE_DRIVER", ex.getMessage(), request.getRequestURI());
    }

    // 403 - Access denied
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            Exception ex, HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You are not authorized", request.getRequestURI());
    }

    // 400 - Validation errors (@Valid bean validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, request.getRequestURI());
    }

    // 400 - Business rule violations (e.g. pickup == destination)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage(), request.getRequestURI());
    }

    // 500 - Internal / State error (e.g. downstream auth issues)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.getMessage(), request.getRequestURI());
    }

    // 503 - Downstream service unavailable
    @ExceptionHandler(org.springframework.web.client.ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable(
            Exception ex, HttpServletRequest request) {
        return buildError(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                "A downstream service is temporarily unavailable", request.getRequestURI());
    }

    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status, String code, String message, String path) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", status.value());
        error.put("error", code);
        error.put("message", message);
        error.put("path", path);
        return ResponseEntity.status(status).body(error);
    }
}
