package com.ridelink.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.ridelink.payment.model.Fare;

public record FareResponse(String id, String rideId, String passengerId, BigDecimal distanceKm,
        BigDecimal baseFare, BigDecimal ratePerKm, BigDecimal totalFare, String currency,
        Instant calculatedAt) {

    public static FareResponse from(Fare f) {
        return new FareResponse(f.getId(), f.getRideId(), f.getPassengerId(), f.getDistanceKm(),
                f.getBaseFare(), f.getRatePerKm(), f.getTotalFare(), f.getCurrency(), f.getCalculatedAt());
    }
}
