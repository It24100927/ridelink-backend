package com.ridelink.driver_vehicle_service.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for POST /api/drivers.
 * accountId is intentionally absent — it is extracted from the JWT subject claim,
 * never accepted from the client.
 */
@Data
public class CreateDriverRequest {

    @NotBlank(message = "serviceArea must not be blank")
    private String serviceArea;

    @NotNull(message = "currentLatitude is required")
    @DecimalMin(value = "-90.0", message = "currentLatitude must be >= -90")
    @DecimalMax(value = "90.0",  message = "currentLatitude must be <= 90")
    private Double currentLatitude;

    @NotNull(message = "currentLongitude is required")
    @DecimalMin(value = "-180.0", message = "currentLongitude must be >= -180")
    @DecimalMax(value = "180.0",  message = "currentLongitude must be <= 180")
    private Double currentLongitude;
}
