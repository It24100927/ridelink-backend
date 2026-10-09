package com.ridelink.driver_vehicle_service.dto.response;

import com.ridelink.driver_vehicle_service.model.Vehicle;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Vehicle response shape.
 * Used by:
 *   POST /api/vehicles             -> 201  (createdAt set; updatedAt equals createdAt on creation)
 *   PUT  /api/vehicles/{vehicleId} -> 200  (both timestamps populated)
 *
 * The internal Mongo {@code id} field is intentionally excluded.
 * Both {@code createdAt} and {@code updatedAt} are included so this single DTO
 * covers both endpoints cleanly.
 */
@Data
@Builder
public class VehicleResponse {

    private String vehicleId;
    private String driverId;
    private String vehicleNumber;
    private String vehicleType;
    private String model;
    private Instant createdAt;
    private Instant updatedAt;

    /** Convenience factory — maps directly from a {@link Vehicle} document. */
    public static VehicleResponse from(Vehicle vehicle) {
        return VehicleResponse.builder()
                .vehicleId(vehicle.getVehicleId())
                .driverId(vehicle.getDriverId())
                .vehicleNumber(vehicle.getVehicleNumber())
                .vehicleType(vehicle.getVehicleType())
                .model(vehicle.getModel())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }
}
