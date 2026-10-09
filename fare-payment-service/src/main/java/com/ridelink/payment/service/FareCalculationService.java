package com.ridelink.payment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.ridelink.payment.config.FareProperties;

/**
 * Implements the fare rule:
 *
 *   fare = baseFare + (distanceKm x ratePerKm)
 *   if fare < minimumFare then fare = minimumFare
 *
 * The result is rounded to 2 decimal places (HALF_UP).
 */
@Service
public class FareCalculationService {

    private final FareProperties properties;

    public FareCalculationService(FareProperties properties) {
        this.properties = properties;
    }

    public BigDecimal calculate(BigDecimal distanceKm) {
        BigDecimal distanceCharge = distanceKm.multiply(properties.getRatePerKm());
        BigDecimal rawFare = properties.getBaseFare().add(distanceCharge);
        return rawFare.max(properties.getMinimumFare()).setScale(2, RoundingMode.HALF_UP);
    }
}
