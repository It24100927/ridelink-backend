package com.ridelink.driver_vehicle_service.repository;

import com.ridelink.driver_vehicle_service.model.Driver;
import com.ridelink.driver_vehicle_service.model.Driver.Availability;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the {@link Driver} document (collection: "drivers").
 *
 * All query methods use Spring Data derived-query naming so no @Query
 * annotations are needed; the field names match exactly those declared
 * on the document.
 */
@Repository
public interface DriverRepository extends MongoRepository<Driver, String> {

    /**
     * Look up a driver by their public driverId (UUID string).
     * Used by: GET, PUT, PATCH endpoints.
     */
    Optional<Driver> findByDriverId(String driverId);

    /**
     * Look up a driver by their accountId (JWT subject).
     * Used by: POST /api/vehicles (derive driverId from authenticated user),
     *          ownership checks in service layer.
     */
    Optional<Driver> findByAccountId(String accountId);

    /**
     * Duplicate-profile guard for POST /api/drivers.
     * Returns true if a Driver document already exists for this accountId.
     */
    boolean existsByAccountId(String accountId);

    /**
     * Eligible-driver query for GET /api/drivers/eligible?serviceArea=X.
     * Returns only drivers that are AVAILABLE in the requested service area.
     * Returns an empty list (never null) when there are no matches.
     */
    List<Driver> findByAvailabilityAndServiceArea(Availability availability, String serviceArea);
}
