package com.ridelink.driver_vehicle_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for PUT /api/drivers/{driverId}.
 * Only serviceArea is updatable via this endpoint.
 */
@Data
public class UpdateServiceAreaRequest {

    @NotBlank(message = "serviceArea must not be blank")
    private String serviceArea;
}
