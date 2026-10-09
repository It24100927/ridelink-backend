package com.ridelink.payment.exception;

/**
 * Thrown when a requested fare or payment does not exist (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
