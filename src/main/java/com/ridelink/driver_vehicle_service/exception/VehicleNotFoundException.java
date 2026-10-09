package com.ridelink.driver_vehicle_service.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a Vehicle document cannot be found by vehicleId. Maps to HTTP 404. */
public class VehicleNotFoundException extends RideLinkException {

    public VehicleNotFoundException(String vehicleId) {
        super("Vehicle not found: " + vehicleId, HttpStatus.NOT_FOUND);
    }
}
