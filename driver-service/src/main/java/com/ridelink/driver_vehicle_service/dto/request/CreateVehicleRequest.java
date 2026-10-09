package com.ridelink.driver_vehicle_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for POST /api/vehicles.
 * driverId is intentionally absent — it is derived server-side from the
 * authenticated driver's accountId (via JWT subject), then used to look up
 * the corresponding Driver record.
 */
@Data
public class CreateVehicleRequest {

    @NotBlank(message = "vehicleNumber must not be blank")
    private String vehicleNumber;

    @NotBlank(message = "vehicleType must not be blank")
    private String vehicleType;

    @NotBlank(message = "model must not be blank")
    private String model;
}
