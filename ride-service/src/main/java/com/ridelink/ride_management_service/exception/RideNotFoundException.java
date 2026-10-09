package com.ridelink.ride_management_service.exception;

public class RideNotFoundException extends RuntimeException {
    public RideNotFoundException(String rideId) {
        super("Ride not found: " + rideId);
    }
}
