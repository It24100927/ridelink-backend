package com.ridelink.ride.dto;

import jakarta.validation.constraints.NotBlank;

public class CancelRideRequest {

    @NotBlank(message = "Passenger ID is required")
    private String passengerId;

    public CancelRideRequest() {
    }

    public String getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(String passengerId) {
        this.passengerId = passengerId;
    }
}