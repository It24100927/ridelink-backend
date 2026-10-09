package com.ridelink.driver_vehicle_service.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a driver profile (Driver document) already exists for the
 * authenticated accountId. Maps to HTTP 409.
 */
public class DuplicateDriverProfileException extends RideLinkException {

    public DuplicateDriverProfileException(String accountId) {
        super("A driver profile already exists for account: " + accountId, HttpStatus.CONFLICT);
    }
}
