package com.ridelink.driver_vehicle_service.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a vehicle registration is attempted with a vehicleNumber that
 * is already registered. Maps to HTTP 409.
 */
public class DuplicateVehicleNumberException extends RideLinkException {

    public DuplicateVehicleNumberException(String vehicleNumber) {
        super("Vehicle number already registered: " + vehicleNumber, HttpStatus.CONFLICT);
    }
}
