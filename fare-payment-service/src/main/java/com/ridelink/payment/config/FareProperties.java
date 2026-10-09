package com.ridelink.payment.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Fare rule values, bound from the "fare.*" properties in application.properties.
 */
@Component
@ConfigurationProperties(prefix = "fare")
public class FareProperties {

    private BigDecimal baseFare = new BigDecimal("150.00");
    private BigDecimal ratePerKm = new BigDecimal("80.00");
    private BigDecimal minimumFare = new BigDecimal("250.00");
    private String currency = "LKR";

    public BigDecimal getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(BigDecimal baseFare) {
        this.baseFare = baseFare;
    }

    public BigDecimal getRatePerKm() {
        return ratePerKm;
    }

    public void setRatePerKm(BigDecimal ratePerKm) {
        this.ratePerKm = ratePerKm;
    }

    public BigDecimal getMinimumFare() {
        return minimumFare;
    }

    public void setMinimumFare(BigDecimal minimumFare) {
        this.minimumFare = minimumFare;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
