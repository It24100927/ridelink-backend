package com.ridelink.payment.exception;

/**
 * Thrown when a request conflicts with the current state, for example
 * a duplicate fare or a payment on an already-paid ride (HTTP 409).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
