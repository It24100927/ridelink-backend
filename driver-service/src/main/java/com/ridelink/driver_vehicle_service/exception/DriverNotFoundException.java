package com.ridelink.driver_vehicle_service.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a Driver document cannot be found by driverId. Maps to HTTP 404. */
public class DriverNotFoundException extends RideLinkException {

    public DriverNotFoundException(String driverId) {
        super("Driver not found: " + driverId, HttpStatus.NOT_FOUND);
    }
}
