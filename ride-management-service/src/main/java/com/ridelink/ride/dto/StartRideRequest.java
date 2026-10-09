package com.ridelink.ride.dto;

import jakarta.validation.constraints.NotBlank;

public class StartRideRequest {

    @NotBlank(message = "Driver ID is required")
    private String driverId;

    public StartRideRequest() {
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }
}