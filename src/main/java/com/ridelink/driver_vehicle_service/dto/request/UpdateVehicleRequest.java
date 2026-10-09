package com.ridelink.driver_vehicle_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for PUT /api/vehicles/{vehicleId}.
 * All three fields are replaceable; all are required and must be non-blank.
 */
@Data
public class UpdateVehicleRequest {

    @NotBlank(message = "vehicleNumber must not be blank")
    private String vehicleNumber;

    @NotBlank(message = "vehicleType must not be blank")
    private String vehicleType;

    @NotBlank(message = "model must not be blank")
    private String model;
}
