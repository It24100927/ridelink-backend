package com.ridelink.driver_vehicle_service.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Standard error response body for all error cases.
 * Shape:
 * {
 *   "timestamp": "2026-09-20T10:30:00Z",
 *   "status":    409,
 *   "error":     "CONFLICT",
 *   "message":   "human readable message",
 *   "path":      "/api/drivers/xyz"
 * }
 *
 * Serialized via GlobalExceptionHandler; never constructed in controllers.
 */
@Data
@Builder
public class ErrorResponse {

    /** ISO-8601 timestamp of when the error occurred. */
    private Instant timestamp;

    /** HTTP status code (e.g. 400, 403, 404, 409). */
    private int status;

    /**
     * Short error label matching the HTTP reason phrase in uppercase
     * (e.g. "NOT_FOUND", "CONFLICT", "BAD_REQUEST", "FORBIDDEN").
     */
    private String error;

    /** Human-readable description of what went wrong. */
    private String message;

    /** Request path that triggered the error. */
    private String path;
}
