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
 * MongoDB document representing a driver's operational profile.
 * One Driver document per Account (accountId is unique).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "drivers")
public class Driver {

    /** Internal Mongo _id — never exposed in API responses. */
    @Id
    private String id;

    /** Public identifier (UUID string), unique. */
    @Indexed(unique = true)
    private String driverId;

    /** Links to the Account Service user; sourced from JWT subject claim. */
    @Indexed(unique = true)
    private String accountId;

    private String serviceArea;

    private Double currentLatitude;

    private Double currentLongitude;

    /**
     * Operational availability.
     * Default on creation: UNAVAILABLE.
     */
    private Availability availability;

    private Instant createdAt;

    private Instant updatedAt;

    public enum Availability {
        AVAILABLE,
        UNAVAILABLE
    }
}
