package com.ridelink.payment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/fares/estimate. Each location is a known place name
 * (for example "Colombo 03") or simulated coordinates written as "lat,lon".
 */
public record FareEstimateRequest(

        @NotBlank(message = "pickup is required")
        String pickup,

        @NotBlank(message = "destination is required")
        String destination) {
}
