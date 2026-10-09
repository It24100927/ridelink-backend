package com.ridelink.ride_management_service.repository;

import com.ridelink.ride_management_service.model.Ride;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideRepository extends MongoRepository<Ride, String> {

    List<Ride> findByPassengerId(String passengerId);

    List<Ride> findByDriverId(String driverId);
}
