package com.ridelink.driver_vehicle_service.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the authenticated user tries to mutate a resource they do not own.
 * Maps to HTTP 403.
 */
public class ForbiddenOperationException extends RideLinkException {

    public ForbiddenOperationException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
