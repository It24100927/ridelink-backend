package com.ridelink.driver.repository;

import com.ridelink.driver.entity.Vehicle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends MongoRepository<Vehicle, String> {
    
    List<Vehicle> findByDriverId(String driverId);
    
    List<Vehicle> findByDriverIdAndIsActiveTrue(String driverId);
    
    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);
    
    boolean existsByRegistrationNumber(String registrationNumber);
    
    Optional<Vehicle> findByIdAndDriverId(String id, String driverId);
}