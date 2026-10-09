package com.ridelink.payment.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.ridelink.payment.config.SecurityConfig;
import com.ridelink.payment.dto.FareEstimateResponse;
import com.ridelink.payment.dto.FareResponse;
import com.ridelink.payment.dto.PaymentResponse;
import com.ridelink.payment.exception.ConflictException;
import com.ridelink.payment.exception.InvalidRequestException;
import com.ridelink.payment.exception.ResourceNotFoundException;
import com.ridelink.payment.model.PaymentMethod;
import com.ridelink.payment.model.PaymentStatus;
import com.ridelink.payment.service.FareService;
import com.ridelink.payment.service.PaymentService;

@WebMvcTest({FareController.class, PaymentController.class})
@Import(SecurityConfig.class)
class SecurityAndApiTest {

    private static final String ESTIMATE_BODY = """
            {"pickup":"Colombo 03","destination":"Kandy Central"}""";
    private static final String FINAL_BODY = """
            {"rideId":"RIDE001","passengerId":"PASS001","actualDistanceKm":2.77}""";
    private static final String PAY_BODY = """
            {"rideId":"RIDE001","method":"CARD","simulateFailure":false}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FareService fareService;

    @MockitoBean
    private PaymentService paymentService;

    private static RequestPostProcessor as(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private FareEstimateResponse estimate() {
        return new FareEstimateResponse("Colombo 03", "Kandy Central", new BigDecimal("2.77"),
                new BigDecimal("150.00"), new BigDecimal("80.00"), new BigDecimal("250.00"),
                new BigDecimal("371.60"), "LKR");
    }

    private FareResponse fare() {
        return new FareResponse("F1", "RIDE001", "PASS001", new BigDecimal("2.77"), new BigDecimal("150.00"),
                new BigDecimal("80.00"), new BigDecimal("371.60"), "LKR", Instant.now());
    }

    private PaymentResponse payment() {
        return new PaymentResponse("PAY001", "RIDE001", "PASS001", new BigDecimal("371.60"), "LKR",
                PaymentMethod.CARD, PaymentStatus.COMPLETED, "RCPT-1", null, Instant.now(), Instant.now());
    }

    @Test
    void noTokenReturns401WithStandardBody() throws Exception {
        mockMvc.perform(post("/api/fares/final").contentType(MediaType.APPLICATION_JSON).content(FINAL_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void estimateWithoutTokenReturns200() throws Exception {
        when(fareService.estimate(any())).thenReturn(estimate());

        mockMvc.perform(post("/api/fares/estimate")
                        .contentType(MediaType.APPLICATION_JSON).content(ESTIMATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedFare").value(371.60))
                .andExpect(jsonPath("$.pickup").value("Colombo 03"))
                .andExpect(jsonPath("$.destination").value("Kandy Central"));
    }

    @Test
    void estimateWithEmptyObjectReturns400() throws Exception {
        mockMvc.perform(post("/api/fares/estimate")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("pickup")))
                .andExpect(jsonPath("$.message").value(containsString("destination")));
    }

    @Test
    void estimateWithUnknownLocationReturns400() throws Exception {
        when(fareService.estimate(any()))
                .thenThrow(new InvalidRequestException("Unknown location: 'Atlantis'"));

        mockMvc.perform(post("/api/fares/estimate")
                        .contentType(MediaType.APPLICATION_JSON).content(ESTIMATE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown location: 'Atlantis'"));
    }

    @Test
    void finalFareWithoutDistanceOrLocationsReturns400() throws Exception {
        when(fareService.calculateFinalFare(any()))
                .thenThrow(new InvalidRequestException("Provide actualDistanceKm, or both pickup and destination"));

        mockMvc.perform(post("/api/fares/final").with(as("DRIVER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rideId\":\"RIDE001\",\"passengerId\":\"PASS001\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownUrlReturns404() throws Exception {
        mockMvc.perform(get("/api/does-not-exist").with(as("PASSENGER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void driverCanSubmitFinalFare() throws Exception {
        when(fareService.calculateFinalFare(any())).thenReturn(fare());

        mockMvc.perform(post("/api/fares/final").with(as("DRIVER"))
                        .contentType(MediaType.APPLICATION_JSON).content(FINAL_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rideId").value("RIDE001"));
    }

    @Test
    void passengerCannotSubmitFinalFare() throws Exception {
        mockMvc.perform(post("/api/fares/final").with(as("PASSENGER"))
                        .contentType(MediaType.APPLICATION_JSON).content(FINAL_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void passengerCanPay() throws Exception {
        when(paymentService.processPayment(any())).thenReturn(payment());

        mockMvc.perform(post("/api/payments").with(as("PASSENGER"))
                        .contentType(MediaType.APPLICATION_JSON).content(PAY_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void driverCannotPay() throws Exception {
        mockMvc.perform(post("/api/payments").with(as("DRIVER"))
                        .contentType(MediaType.APPLICATION_JSON).content(PAY_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void anyAuthenticatedUserCanReadPayment() throws Exception {
        when(paymentService.getPayment("PAY001")).thenReturn(payment());

        mockMvc.perform(get("/api/payments/PAY001").with(as("DRIVER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("PAY001"));
    }

    @Test
    void readingWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/fares/ride/RIDE001")).andExpect(status().isUnauthorized());
    }

    @Test
    void negativeDistanceReturns400() throws Exception {
        String body = """
                {"rideId":"RIDE001","passengerId":"PASS001","actualDistanceKm":-1}""";

        mockMvc.perform(post("/api/fares/final").with(as("DRIVER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownPaymentMethodReturns400() throws Exception {
        String body = """
                {"rideId":"RIDE001","method":"BITCOIN","simulateFailure":false}""";

        mockMvc.perform(post("/api/payments").with(as("PASSENGER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/payments").with(as("PASSENGER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingFareReturns404() throws Exception {
        when(fareService.getFareByRideId("NOPE"))
                .thenThrow(new ResourceNotFoundException("No fare found for ride NOPE"));

        mockMvc.perform(get("/api/fares/ride/NOPE").with(as("PASSENGER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No fare found for ride NOPE"));
    }

    @Test
    void duplicatePaymentReturns409() throws Exception {
        when(paymentService.processPayment(any()))
                .thenThrow(new ConflictException("Ride RIDE001 has already been paid"));

        mockMvc.perform(post("/api/payments").with(as("PASSENGER"))
                        .contentType(MediaType.APPLICATION_JSON).content(PAY_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(get("/api/fares/estimate").with(as("PASSENGER")))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unsupportedMediaTypeReturns415() throws Exception {
        mockMvc.perform(post("/api/fares/estimate")
                        .contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
