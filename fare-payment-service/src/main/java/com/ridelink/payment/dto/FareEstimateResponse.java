package com.ridelink.payment.dto;

import java.math.BigDecimal;

/**
 * Response body of POST /api/fares/estimate. Nothing is stored for estimates.
 * pickup and destination are echoed from the request.
 */
public record FareEstimateResponse(
        String pickup,
        String destination,
        BigDecimal distanceKm,
        BigDecimal baseFare,
        BigDecimal ratePerKm,
        BigDecimal minimumFare,
        BigDecimal estimatedFare,
        String currency) {
}
