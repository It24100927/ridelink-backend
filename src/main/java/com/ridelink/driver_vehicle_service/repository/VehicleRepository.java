package com.ridelink.driver_vehicle_service.repository;

import com.ridelink.driver_vehicle_service.model.Vehicle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for the {@link Vehicle} document (collection: "vehicles").
 *
 * All query methods use Spring Data derived-query naming; field names
 * match exactly those declared on the document.
 */
@Repository
public interface VehicleRepository extends MongoRepository<Vehicle, String> {

    /**
     * Look up a vehicle by its public vehicleId (UUID string).
     * Used by: PUT /api/vehicles/{vehicleId}.
     */
    Optional<Vehicle> findByVehicleId(String vehicleId);

    /**
     * Duplicate vehicleNumber guard for POST /api/vehicles.
     * Returns true if a Vehicle with this plate is already registered.
     */
    boolean existsByVehicleNumber(String vehicleNumber);
}
