package com.ridelink.payment.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ridelink.payment.model.Payment;
import com.ridelink.payment.model.PaymentStatus;

/**
 * Database access for the "payments" collection.
 */
public interface PaymentRepository extends MongoRepository<Payment, String> {

    List<Payment> findByRideIdOrderByCreatedAtDesc(String rideId);

    boolean existsByRideIdAndStatus(String rideId, PaymentStatus status);
}
