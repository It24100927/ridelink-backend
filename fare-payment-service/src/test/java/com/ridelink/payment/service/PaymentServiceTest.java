package com.ridelink.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.dto.PaymentResponse;
import com.ridelink.payment.dto.ReceiptResponse;
import com.ridelink.payment.exception.ConflictException;
import com.ridelink.payment.exception.ResourceNotFoundException;
import com.ridelink.payment.model.Fare;
import com.ridelink.payment.model.Payment;
import com.ridelink.payment.model.PaymentMethod;
import com.ridelink.payment.model.PaymentStatus;
import com.ridelink.payment.repository.FareRepository;
import com.ridelink.payment.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FareRepository fareRepository;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, fareRepository);
    }

    private Fare fare() {
        Fare fare = new Fare();
        fare.setRideId("RIDE001");
        fare.setPassengerId("PASS001");
        fare.setDistanceKm(new BigDecimal("2.77"));
        fare.setBaseFare(new BigDecimal("150.00"));
        fare.setRatePerKm(new BigDecimal("80.00"));
        fare.setTotalFare(new BigDecimal("371.60"));
        fare.setCurrency("LKR");
        return fare;
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId("PAY001");
        payment.setRideId("RIDE001");
        payment.setPassengerId("PASS001");
        payment.setAmount(new BigDecimal("371.60"));
        payment.setCurrency("LKR");
        payment.setMethod(PaymentMethod.CARD);
        payment.setStatus(status);
        payment.setCreatedAt(Instant.now());
        if (status == PaymentStatus.COMPLETED) {
            payment.setReceiptNumber("RCPT-20260101-ABCD1234");
            payment.setPaidAt(Instant.now());
        }
        return payment;
    }

    private void stubFareAndNoCompletedPayment() {
        when(fareRepository.findByRideId("RIDE001")).thenReturn(Optional.of(fare()));
        when(paymentRepository.existsByRideIdAndStatus("RIDE001", PaymentStatus.COMPLETED)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void successfulPaymentIsCompletedWithReceiptAndFareAmount() {
        stubFareAndNoCompletedPayment();

        PaymentResponse response = service.processPayment(new PaymentRequest("RIDE001", PaymentMethod.CARD, false));

        assertThat(response.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.amount()).isEqualByComparingTo("371.60");
        assertThat(response.currency()).isEqualTo("LKR");
        assertThat(response.passengerId()).isEqualTo("PASS001");
        assertThat(response.receiptNumber()).startsWith("RCPT-");
        assertThat(response.paidAt()).isNotNull();
        assertThat(response.failureReason()).isNull();
    }

    @Test
    void simulatedFailureIsFailedWithoutReceipt() {
        stubFareAndNoCompletedPayment();

        PaymentResponse response = service.processPayment(new PaymentRequest("RIDE001", PaymentMethod.CARD, true));

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.failureReason()).isNotBlank();
        assertThat(response.receiptNumber()).isNull();
        assertThat(response.paidAt()).isNull();
    }

    @Test
    void failedPaymentCanBeRetried() {
        // A FAILED attempt exists but no COMPLETED one, so a new attempt is allowed.
        stubFareAndNoCompletedPayment();

        PaymentResponse failed = service.processPayment(new PaymentRequest("RIDE001", PaymentMethod.CARD, true));
        PaymentResponse retry = service.processPayment(new PaymentRequest("RIDE001", PaymentMethod.CASH, false));

        assertThat(failed.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(retry.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(retry.method()).isEqualTo(PaymentMethod.CASH);
    }

    @Test
    void completedPaymentCannotBeDuplicated() {
        when(fareRepository.findByRideId("RIDE001")).thenReturn(Optional.of(fare()));
        when(paymentRepository.existsByRideIdAndStatus("RIDE001", PaymentStatus.COMPLETED)).thenReturn(true);

        assertThatThrownBy(() -> service.processPayment(new PaymentRequest("RIDE001", PaymentMethod.CARD, false)))
                .isInstanceOf(ConflictException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void missingFarePreventsPayment() {
        when(fareRepository.findByRideId("RIDE001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processPayment(new PaymentRequest("RIDE001", PaymentMethod.CARD, false)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void receiptIsAvailableForCompletedPayment() {
        when(paymentRepository.findById("PAY001")).thenReturn(Optional.of(payment(PaymentStatus.COMPLETED)));
        when(fareRepository.findByRideId("RIDE001")).thenReturn(Optional.of(fare()));

        ReceiptResponse receipt = service.getReceipt("PAY001");

        assertThat(receipt.receiptNumber()).isEqualTo("RCPT-20260101-ABCD1234");
        assertThat(receipt.distanceKm()).isEqualByComparingTo("2.77");
        assertThat(receipt.baseFare()).isEqualByComparingTo("150.00");
        assertThat(receipt.ratePerKm()).isEqualByComparingTo("80.00");
        assertThat(receipt.totalPaid()).isEqualByComparingTo("371.60");
        assertThat(receipt.method()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    void receiptIsRejectedForFailedPayment() {
        when(paymentRepository.findById("PAY001")).thenReturn(Optional.of(payment(PaymentStatus.FAILED)));

        assertThatThrownBy(() -> service.getReceipt("PAY001")).isInstanceOf(ConflictException.class);
    }

    @Test
    void receiptIsRejectedForPendingPayment() {
        when(paymentRepository.findById("PAY001")).thenReturn(Optional.of(payment(PaymentStatus.PENDING)));

        assertThatThrownBy(() -> service.getReceipt("PAY001")).isInstanceOf(ConflictException.class);
    }

    @Test
    void unknownPaymentThrowsNotFound() {
        when(paymentRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPayment("NOPE")).isInstanceOf(ResourceNotFoundException.class);
    }
}
