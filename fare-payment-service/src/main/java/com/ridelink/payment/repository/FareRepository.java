package com.ridelink.payment.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ridelink.payment.model.Fare;

/**
 * Database access for the "fares" collection.
 */
public interface FareRepository extends MongoRepository<Fare, String> {

    Optional<Fare> findByRideId(String rideId);

    boolean existsByRideId(String rideId);
}
