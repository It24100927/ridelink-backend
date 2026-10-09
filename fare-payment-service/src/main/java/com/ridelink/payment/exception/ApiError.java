package com.ridelink.payment.exception;

import java.time.Instant;

/**
 * Consistent JSON error body returned by every failing endpoint.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message) {
}
