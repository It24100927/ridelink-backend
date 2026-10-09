package com.ridelink.driver_vehicle_service.dto.response;

import com.ridelink.driver_vehicle_service.model.Driver;
import com.ridelink.driver_vehicle_service.model.Driver.Availability;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Full driver response shape.
 * Used by:
 *   POST /api/drivers          -> 201
 *   GET  /api/drivers/{id}     -> 200
 *   PUT  /api/drivers/{id}     -> 200
 *
 * The internal Mongo {@code id} field is intentionally excluded.
 */
@Data
@Builder
public class DriverResponse {

    private String driverId;
    private String accountId;
    private String serviceArea;
    private Double currentLatitude;
    private Double currentLongitude;
    private Availability availability;
    private Instant createdAt;
    private Instant updatedAt;

    /** Convenience factory — maps directly from a {@link Driver} document. */
    public static DriverResponse from(Driver driver) {
        return DriverResponse.builder()
                .driverId(driver.getDriverId())
                .accountId(driver.getAccountId())
                .serviceArea(driver.getServiceArea())
                .currentLatitude(driver.getCurrentLatitude())
                .currentLongitude(driver.getCurrentLongitude())
                .availability(driver.getAvailability())
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }
}
