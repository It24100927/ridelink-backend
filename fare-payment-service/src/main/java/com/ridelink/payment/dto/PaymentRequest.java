package com.ridelink.payment.dto;

import com.ridelink.payment.model.PaymentMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for POST /api/payments.
 * The amount is not accepted from the client; it always comes from the stored fare.
 * simulateFailure is optional: when it is missing from the JSON it defaults to false.
 * Set it to true to demonstrate a failed simulated payment.
 */
public record PaymentRequest(

        @NotBlank(message = "rideId is required")
        String rideId,

        @NotNull(message = "method is required (CASH or CARD)")
        PaymentMethod method,

        Boolean simulateFailure) {

    public PaymentRequest {
        if (simulateFailure == null) {
            simulateFailure = Boolean.FALSE;
        }
    }
}