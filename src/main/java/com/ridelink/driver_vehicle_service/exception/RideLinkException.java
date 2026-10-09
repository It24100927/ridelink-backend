package com.ridelink.driver_vehicle_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Base class for all RideLink service exceptions.
 * Keeps the exception hierarchy extensible (e.g. for future downstream
 * 503-style exceptions such as ExternalServiceUnavailableException).
 */
public abstract class RideLinkException extends RuntimeException {

    private final HttpStatus status;

    protected RideLinkException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
