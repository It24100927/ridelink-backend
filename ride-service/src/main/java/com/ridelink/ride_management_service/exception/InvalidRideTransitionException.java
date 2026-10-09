package com.ridelink.ride_management_service.exception;

import com.ridelink.ride_management_service.enums.RideStatus;

public class InvalidRideTransitionException extends RuntimeException {
    public InvalidRideTransitionException(RideStatus current, RideStatus target) {
        super("Cannot transition ride from " + current + " to " + target);
    }
}
