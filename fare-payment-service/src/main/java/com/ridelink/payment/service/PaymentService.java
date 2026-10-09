package com.ridelink.payment.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.dto.PaymentResponse;
import com.ridelink.payment.dto.ReceiptResponse;
import com.ridelink.payment.exception.ConflictException;
import com.ridelink.payment.exception.ResourceNotFoundException;
import com.ridelink.payment.model.Fare;
import com.ridelink.payment.model.Payment;
import com.ridelink.payment.model.PaymentStatus;
import com.ridelink.payment.repository.FareRepository;
import com.ridelink.payment.repository.PaymentRepository;

/**
 * Simulated payment processing, payment lookup and receipt generation.
 * No real payment gateway or financial data is involved.
 */
@Service
public class PaymentService {

    private static final DateTimeFormatter RECEIPT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PaymentRepository paymentRepository;
    private final FareRepository fareRepository;

    public PaymentService(PaymentRepository paymentRepository, FareRepository fareRepository) {
        this.paymentRepository = paymentRepository;
        this.fareRepository = fareRepository;
    }

    /**
     * Processes a simulated payment for a ride. The amount always comes from the stored fare.
     *
     * @throws ResourceNotFoundException if the ride has no final fare yet
     * @throws ConflictException         if the ride has already been paid
     */
    public PaymentResponse processPayment(PaymentRequest request) {
        Fare fare = fareRepository.findByRideId(request.rideId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No final fare found for ride " + request.rideId()
                                + ". Calculate the final fare before paying."));

        if (paymentRepository.existsByRideIdAndStatus(request.rideId(), PaymentStatus.COMPLETED)) {
            throw new ConflictException("Ride " + request.rideId() + " has already been paid");
        }

        Payment payment = new Payment();
        payment.setRideId(fare.getRideId());
        payment.setPassengerId(fare.getPassengerId());
        payment.setAmount(fare.getTotalFare());
        payment.setCurrency(fare.getCurrency());
        payment.setMethod(request.method());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(Instant.now());
        payment = paymentRepository.save(payment);

        if (request.simulateFailure()) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Simulated payment failure");
        } else {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(Instant.now());
            payment.setReceiptNumber(generateReceiptNumber());
        }

        return PaymentResponse.from(paymentRepository.save(payment));
    }

    /**
     * @throws ResourceNotFoundException if the payment does not exist
     */
    public PaymentResponse getPayment(String paymentId) {
        return PaymentResponse.from(findPayment(paymentId));
    }

    /** All payment attempts for a ride, newest first. Empty list if none. */
    public List<PaymentResponse> getPaymentsByRideId(String rideId) {
        return paymentRepository.findByRideIdOrderByCreatedAtDesc(rideId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    /**
     * Builds the receipt for a completed payment.
     *
     * @throws ResourceNotFoundException if the payment or its fare does not exist
     * @throws ConflictException         if the payment is not COMPLETED
     */
    public ReceiptResponse getReceipt(String paymentId) {
        Payment payment = findPayment(paymentId);

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new ConflictException("Receipt is only available for COMPLETED payments. "
                    + "This payment is " + payment.getStatus());
        }

        Fare fare = fareRepository.findByRideId(payment.getRideId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fare record missing for ride " + payment.getRideId()));

        return new ReceiptResponse(
                payment.getReceiptNumber(),
                payment.getId(),
                payment.getRideId(),
                payment.getPassengerId(),
                fare.getDistanceKm(),
                fare.getBaseFare(),
                fare.getRatePerKm(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod(),
                payment.getPaidAt());
    }

    private Payment findPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
    }

    private String generateReceiptNumber() {
        String date = LocalDate.now(ZoneOffset.UTC).format(RECEIPT_DATE);
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "RCPT-" + date + "-" + suffix;
    }
}
