package com.ridelink.driver_vehicle_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document representing a vehicle registered by a driver.
 * vehicleType is stored as a free-text String (e.g. "SEDAN", "VAN", "TUK")
 * rather than an enum so that new vehicle types can be added without a
 * schema migration.
 * model is a single combined field (e.g. "Toyota Prius") — not split into
 * make + model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vehicles")
public class Vehicle {

    /** Internal Mongo _id — never exposed in API responses. */
    @Id
    private String id;

    /** Public identifier (UUID string), unique. */
    @Indexed(unique = true)
    private String vehicleId;

    /** References Driver.driverId (not the Mongo _id). */
    @Indexed
    private String driverId;

    @Indexed(unique = true)
    private String vehicleNumber;

    /**
     * Free-text vehicle type (e.g. "SEDAN", "VAN", "TUK").
     * Validated as non-blank by the DTO layer, not restricted to an enum,
     * to allow easy extension without a schema migration.
     */
    private String vehicleType;

    /**
     * Combined make + model string (e.g. "Toyota Prius").
     * Not split into separate make/model fields per the contract.
     */
    private String model;

    private Instant createdAt;

    private Instant updatedAt;
}
