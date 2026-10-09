package com.ridelink.driver_vehicle_service.exception;

import com.ridelink.driver_vehicle_service.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Centralised exception → HTTP response mapper.
 *
 * Every handler produces the standard error shape:
 * {
 *   "timestamp": "<ISO-8601>",
 *   "status":    <int>,
 *   "error":     "<LABEL>",
 *   "message":   "<human readable>",
 *   "path":      "<request URI>"
 * }
 *
 * No raw stack traces or default Spring error bodies ever reach the client.
 *
 * Handler ordering (most-specific → least-specific):
 *   1. RideLinkException subtypes  (domain exceptions with built-in status)
 *   2. MethodArgumentNotValidException  (@Valid failures)
 *   3. HttpMessageNotReadableException  (bad JSON / invalid enum)
 *   4. IllegalArgumentException         (blank serviceArea query param)
 *   5. Fallback Exception               (unexpected 500)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -----------------------------------------------------------------------
    // 1.  Domain exceptions — all extend RideLinkException which carries status
    // -----------------------------------------------------------------------

    /**
     * Handles:
     *   DriverNotFoundException          → 404
     *   VehicleNotFoundException         → 404
     *   DuplicateDriverProfileException  → 409
     *   DuplicateVehicleNumberException  → 409
     *   ForbiddenOperationException      → 403
     *   (and any future RideLinkException subclasses)
     */
    @ExceptionHandler(RideLinkException.class)
    public ResponseEntity<ErrorResponse> handleRideLinkException(
            RideLinkException ex,
            HttpServletRequest request) {

        HttpStatus status = ex.getStatus();
        return buildResponse(status, ex.getMessage(), request);
    }

    // -----------------------------------------------------------------------
    // 2.  Bean-validation failure  (@Valid on request body DTOs)
    // -----------------------------------------------------------------------

    /**
     * Fired by Spring MVC when a {@code @Valid} annotated request body fails
     * Jakarta Bean Validation constraints (@NotBlank, @DecimalMin, @DecimalMax, etc.).
     * Collects all field-level errors into a single comma-separated message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    // -----------------------------------------------------------------------
    // 3.  Unreadable / unparseable request body  (bad JSON or invalid enum)
    // -----------------------------------------------------------------------

    /**
     * Fired when Jackson cannot deserialise the request body.
     * Common causes:
     *  - Malformed JSON syntax.
     *  - An enum field contains a string that isn't one of the enum constants
     *    (e.g. availability = "BUSY" instead of "AVAILABLE"/"UNAVAILABLE").
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        // Provide a safe, human-readable message without leaking internal detail
        String message = "Request body is malformed or contains an invalid value. "
                + "Check that enum fields use allowed values (e.g. AVAILABLE, UNAVAILABLE).";
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    // -----------------------------------------------------------------------
    // 4.  Blank / missing query parameter
    // -----------------------------------------------------------------------

    /**
     * Fired by DriverController when the {@code serviceArea} query param on
     * {@code GET /api/drivers/eligible} is missing or blank.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // -----------------------------------------------------------------------
    // 5.  Fallback — unexpected server errors
    // -----------------------------------------------------------------------

    /**
     * Safety net for any exception not handled above.
     * Returns 500 with a generic message so internal details are never leaked.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception on [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                request);
    }

    // -----------------------------------------------------------------------
    // Shared builder
    // -----------------------------------------------------------------------

    /**
     * Builds the standard {@link ErrorResponse} and wraps it in a
     * {@link ResponseEntity} with the given status.
     *
     * @param status  the HTTP status to use for both the response code and
     *                the {@code status} / {@code error} fields in the body
     * @param message human-readable description of the problem
     * @param request the current servlet request (used to populate {@code path})
     */
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status,
                                                         String message,
                                                         HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.name())           // e.g. "NOT_FOUND", "CONFLICT", "BAD_REQUEST"
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(body);
    }
}
