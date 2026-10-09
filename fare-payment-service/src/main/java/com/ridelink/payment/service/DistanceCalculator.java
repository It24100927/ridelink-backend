package com.ridelink.payment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

/**
 * Calculates the great-circle distance between two coordinates (Haversine formula).
 */
@Component
public class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    /**
     * @return distance in kilometres, rounded to 2 decimal places
     */
    public BigDecimal distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return BigDecimal.valueOf(EARTH_RADIUS_KM * c).setScale(2, RoundingMode.HALF_UP);
    }
}
