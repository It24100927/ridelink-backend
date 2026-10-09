package com.ridelink.payment.exception;

/**
 * Thrown when a request is well-formed JSON but cannot be processed, for example
 * an unknown location or a final fare request without a distance (HTTP 400).
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
