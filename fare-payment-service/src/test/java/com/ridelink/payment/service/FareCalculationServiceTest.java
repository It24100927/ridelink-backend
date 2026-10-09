package com.ridelink.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.ridelink.payment.config.FareProperties;

class FareCalculationServiceTest {

    // Defaults of FareProperties: base 150.00, rate 80.00/km, minimum 250.00
    private final FareCalculationService service = new FareCalculationService(new FareProperties());

    @Test
    void zeroDistanceChargesMinimumFare() {
        assertThat(service.calculate(BigDecimal.ZERO)).isEqualByComparingTo("250.00");
    }

    @Test
    void fareBelowMinimumIsRaisedToMinimum() {
        // 150 + 1.00 x 80 = 230 < 250
        assertThat(service.calculate(new BigDecimal("1.00"))).isEqualByComparingTo("250.00");
    }

    @Test
    void fareExactlyAtMinimumBoundary() {
        // 150 + 1.25 x 80 = 250
        assertThat(service.calculate(new BigDecimal("1.25"))).isEqualByComparingTo("250.00");
    }

    @Test
    void justAboveMinimumBoundaryIsChargedByFormula() {
        // 150 + 1.26 x 80 = 250.80
        assertThat(service.calculate(new BigDecimal("1.26"))).isEqualByComparingTo("250.80");
    }

    @Test
    void largeDistanceUsesFormula() {
        // 150 + 500 x 80 = 40150
        assertThat(service.calculate(new BigDecimal("500"))).isEqualByComparingTo("40150.00");
    }

    @Test
    void fareAboveMinimumUsesFormula() {
        // 150 + 10 x 80 = 950
        assertThat(service.calculate(new BigDecimal("10"))).isEqualByComparingTo("950.00");
    }

    @Test
    void decimalDistance() {
        // 150 + 2.77 x 80 = 371.60
        assertThat(service.calculate(new BigDecimal("2.77"))).isEqualByComparingTo("371.60");
    }

    @Test
    void resultIsRoundedHalfUpToTwoDecimals() {
        // 150 + 2.7706 x 80 = 371.648 -> 371.65
        BigDecimal fare = service.calculate(new BigDecimal("2.7706"));
        assertThat(fare).isEqualByComparingTo("371.65");
        assertThat(fare.scale()).isEqualTo(2);
    }
}
