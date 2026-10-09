package com.ridelink.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class DistanceCalculatorTest {

    private final DistanceCalculator calculator = new DistanceCalculator();

    @Test
    void normalCoordinatesGiveExpectedDistance() {
        BigDecimal km = calculator.distanceKm(6.9271, 79.8612, 6.9022, 79.8613);
        assertThat(km).isEqualByComparingTo("2.77");
    }

    @Test
    void samePickupAndDestinationIsZero() {
        BigDecimal km = calculator.distanceKm(6.9271, 79.8612, 6.9271, 79.8612);
        assertThat(km).isEqualByComparingTo("0.00");
    }

    @Test
    void knownColomboPairIsAboutOnePointEightKm() {
        // Colombo 03 (6.9147, 79.8523) to Colombo 07 (6.9023, 79.8616)
        BigDecimal km = calculator.distanceKm(6.9147, 79.8523, 6.9023, 79.8616);
        assertThat(km).isBetween(new BigDecimal("1.50"), new BigDecimal("2.00"));
    }

    @Test
    void boundaryCoordinatesAreAccepted() {
        // Pole to pole is half the circumference: pi x 6371.0088
        assertThat(calculator.distanceKm(90, 0, -90, 0)).isEqualByComparingTo("20015.11");
        // 180 and -180 are the same meridian
        assertThat(calculator.distanceKm(0, 180, 0, -180)).isEqualByComparingTo("0.00");
    }

    @Test
    void resultHasTwoDecimalPlaces() {
        assertThat(calculator.distanceKm(6.9271, 79.8612, 7.2906, 80.6337).scale()).isEqualTo(2);
    }
}
