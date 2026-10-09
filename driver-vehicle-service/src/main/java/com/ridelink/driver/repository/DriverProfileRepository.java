package com.ridelink.driver.repository;

import com.ridelink.driver.entity.DriverProfile;
import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.enums.DriverStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverProfileRepository extends MongoRepository<DriverProfile, String> {

    Optional<DriverProfile> findByAccountId(String accountId);
    Optional<DriverProfile> findByLicenseNumber(String licenseNumber);
    boolean existsByAccountId(String accountId);
    boolean existsByLicenseNumber(String licenseNumber);
    List<DriverProfile> findByStatus(DriverStatus status);
    List<DriverProfile> findByAvailabilityStatus(AvailabilityStatus availabilityStatus);

    @Query("{ 'status': ?0, 'availabilityStatus': ?1 }")
    List<DriverProfile> findEligibleDrivers(DriverStatus status, AvailabilityStatus availability);

    @Query("{ 'status': 'ACTIVE', 'availabilityStatus': 'AVAILABLE' }")
    List<DriverProfile> findAllAvailableDrivers();

    @Query("{ 'serviceAreas': { $in: ?0 }, 'status': 'ACTIVE', 'availabilityStatus': 'AVAILABLE' }")
    List<DriverProfile> findByServiceAreas(List<String> serviceAreas);
}