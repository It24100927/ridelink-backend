package com.ridelink.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ridelink.payment.config.SecurityConfig;
import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.dto.PaymentResponse;
import com.ridelink.payment.model.PaymentMethod;
import com.ridelink.payment.model.PaymentStatus;
import com.ridelink.payment.service.PaymentService;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentRequestJsonTest {

    private static final String WITHOUT_FLAG = """
            {"rideId":"RIDE001","method":"CARD"}""";
    private static final String WITH_FLAG_TRUE = """
            {"rideId":"RIDE001","method":"CARD","simulateFailure":true}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    private static PaymentResponse completed() {
        return new PaymentResponse("PAY001", "RIDE001", "PASS001", new BigDecimal("371.60"), "LKR",
                PaymentMethod.CARD, PaymentStatus.COMPLETED, "RCPT-1", null, Instant.now(), Instant.now());
    }

    private PaymentRequest postAsPassenger(String body) throws Exception {
        when(paymentService.processPayment(any())).thenReturn(completed());

        mockMvc.perform(post("/api/payments")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PASSENGER")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentService).processPayment(captor.capture());
        return captor.getValue();
    }

    @Test
    void simulateFailureIsOptionalAndDefaultsToFalse() throws Exception {
        assertThat(postAsPassenger(WITHOUT_FLAG).simulateFailure()).isFalse();
    }

    @Test
    void simulateFailureTrueIsHonoured() throws Exception {
        assertThat(postAsPassenger(WITH_FLAG_TRUE).simulateFailure()).isTrue();
    }
}