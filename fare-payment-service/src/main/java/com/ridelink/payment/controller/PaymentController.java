package com.ridelink.payment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.dto.PaymentResponse;
import com.ridelink.payment.dto.ReceiptResponse;
import com.ridelink.payment.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST endpoints for simulated payments and receipts.
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Simulated payments, payment status and receipts")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Record a simulated payment for a ride",
            description = "The amount always comes from the stored final fare. The response status is "
                    + "COMPLETED (receipt issued) or FAILED (when simulateFailure is true). "
                    + "A FAILED payment can be retried. Roles: PASSENGER, ADMIN.")
    @ApiResponse(responseCode = "201", description = "Payment recorded (COMPLETED or FAILED)")
    @ApiResponse(responseCode = "400", description = "Invalid input (blank rideId or unknown method)")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    @ApiResponse(responseCode = "403", description = "Role not allowed")
    @ApiResponse(responseCode = "404", description = "No final fare exists for this ride")
    @ApiResponse(responseCode = "409", description = "Ride has already been paid")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(@Valid @RequestBody PaymentRequest request) {
        return paymentService.processPayment(request);
    }

    @Operation(summary = "Get a payment and its status", description = "Any authenticated user.")
    @ApiResponse(responseCode = "200", description = "Payment found")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@PathVariable("paymentId") String paymentId) {
        return paymentService.getPayment(paymentId);
    }

    @Operation(summary = "List payment attempts for a ride, newest first",
            description = "Shows failed attempts, retries and the completed payment. Any authenticated user.")
    @ApiResponse(responseCode = "200", description = "Payment attempts (empty list if none)")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    @GetMapping("/ride/{rideId}")
    public List<PaymentResponse> getPaymentsByRide(@PathVariable("rideId") String rideId) {
        return paymentService.getPaymentsByRideId(rideId);
    }

    @Operation(summary = "Get the receipt of a completed payment", description = "Any authenticated user.")
    @ApiResponse(responseCode = "200", description = "Receipt generated")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    @ApiResponse(responseCode = "409", description = "Payment is not COMPLETED")
    @GetMapping("/{paymentId}/receipt")
    public ReceiptResponse getReceipt(@PathVariable("paymentId") String paymentId) {
        return paymentService.getReceipt(paymentId);
    }
}
