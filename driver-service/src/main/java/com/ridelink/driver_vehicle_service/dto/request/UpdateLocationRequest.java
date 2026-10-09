package com.ridelink.driver_vehicle_service.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for PATCH /api/drivers/{driverId}/location.
 */
@Data
public class UpdateLocationRequest {

    @NotNull(message = "currentLatitude is required")
    @DecimalMin(value = "-90.0", message = "currentLatitude must be >= -90")
    @DecimalMax(value = "90.0",  message = "currentLatitude must be <= 90")
    private Double currentLatitude;

    @NotNull(message = "currentLongitude is required")
    @DecimalMin(value = "-180.0", message = "currentLongitude must be >= -180")
    @DecimalMax(value = "180.0",  message = "currentLongitude must be <= 180")
    private Double currentLongitude;
}
