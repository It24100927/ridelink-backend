package com.ridelink.driver_vehicle_service.dto.response;

import com.ridelink.driver_vehicle_service.model.Driver;
import com.ridelink.driver_vehicle_service.model.Driver.Availability;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Partial response for PATCH /api/drivers/{driverId}/availability.
 * Contract shape: { "driverId", "availability", "updatedAt" }
 */
@Data
@Builder
public class AvailabilityResponse {

    private String driverId;
    private Availability availability;
    private Instant updatedAt;

    public static AvailabilityResponse from(Driver driver) {
        return AvailabilityResponse.builder()
                .driverId(driver.getDriverId())
                .availability(driver.getAvailability())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }
}
