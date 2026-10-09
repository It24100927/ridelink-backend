package com.ridelink.ride.dto;

import jakarta.validation.constraints.NotBlank;

public class CompleteRideRequest {

    @NotBlank(message = "Driver ID is required")
    private String driverId;

    public CompleteRideRequest() {
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }
}