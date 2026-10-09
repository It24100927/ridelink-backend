package com.ridelink.driver_vehicle_service.dto.response;

import com.ridelink.driver_vehicle_service.model.Driver;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Partial response for PATCH /api/drivers/{driverId}/location.
 * Contract shape: { "driverId", "currentLatitude", "currentLongitude", "updatedAt" }
 */
@Data
@Builder
public class LocationResponse {

    private String driverId;
    private Double currentLatitude;
    private Double currentLongitude;
    private Instant updatedAt;

    public static LocationResponse from(Driver driver) {
        return LocationResponse.builder()
                .driverId(driver.getDriverId())
                .currentLatitude(driver.getCurrentLatitude())
                .currentLongitude(driver.getCurrentLongitude())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }
}
