package com.ridelink.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.ridelink.payment.model.Payment;
import com.ridelink.payment.model.PaymentMethod;
import com.ridelink.payment.model.PaymentStatus;

public record PaymentResponse(String id, String rideId, String passengerId, BigDecimal amount,
        String currency, PaymentMethod method, PaymentStatus status, String receiptNumber,
        String failureReason, Instant createdAt, Instant paidAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getRideId(), p.getPassengerId(), p.getAmount(),
                p.getCurrency(), p.getMethod(), p.getStatus(), p.getReceiptNumber(),
                p.getFailureReason(), p.getCreatedAt(), p.getPaidAt());
    }
}
