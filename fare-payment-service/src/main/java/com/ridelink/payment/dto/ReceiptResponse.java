package com.ridelink.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.ridelink.payment.model.PaymentMethod;

/**
 * Receipt for a COMPLETED payment, including the fare breakdown.
 */
public record ReceiptResponse(
        String receiptNumber,
        String paymentId,
        String rideId,
        String passengerId,
        BigDecimal distanceKm,
        BigDecimal baseFare,
        BigDecimal ratePerKm,
        BigDecimal totalPaid,
        String currency,
        PaymentMethod method,
        Instant paidAt) {
}
