package com.ridelink.payment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/fares/final, sent when a ride is completed.
 * Either actualDistanceKm is given, or both pickup and destination are given
 * (the distance is then calculated from the two locations).
 */
public record FinalFareRequest(

        @NotBlank(message = "rideId is required")
        String rideId,

        @NotBlank(message = "passengerId is required")
        String passengerId,

        @DecimalMin(value = "0.0", message = "actualDistanceKm cannot be negative")
        BigDecimal actualDistanceKm,

        String pickup,

        String destination) {
}
