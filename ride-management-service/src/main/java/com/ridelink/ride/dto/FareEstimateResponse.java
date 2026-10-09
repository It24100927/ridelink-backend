package com.ridelink.ride.dto;

import java.math.BigDecimal;

public class FareEstimateResponse {

    private BigDecimal estimatedFare;

    public FareEstimateResponse() {
    }

    public BigDecimal getEstimatedFare() {
        return estimatedFare;
    }

    public void setEstimatedFare(BigDecimal estimatedFare) {
        this.estimatedFare = estimatedFare;
    }
}