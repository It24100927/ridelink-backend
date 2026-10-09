package com.ridelink.driver_vehicle_service.dto.request;

import com.ridelink.driver_vehicle_service.model.Driver.Availability;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for PATCH /api/drivers/{driverId}/availability.
 *
 * The field type is the {@link Availability} enum directly.
 * Jackson will throw {@code HttpMessageNotReadableException} for any string value
 * that is not "AVAILABLE" or "UNAVAILABLE", which GlobalExceptionHandler converts
 * to a 400 response with the standard error shape.
 */
@Data
public class UpdateAvailabilityRequest {

    @NotNull(message = "availability must be AVAILABLE or UNAVAILABLE")
    private Availability availability;
}
