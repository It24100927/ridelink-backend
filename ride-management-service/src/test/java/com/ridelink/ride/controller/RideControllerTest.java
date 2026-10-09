package com.ridelink.ride.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.ride.dto.*;
import com.ridelink.ride.exception.GlobalExceptionHandler;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.service.RideService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RideControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RideService rideService;

    @InjectMocks
    private RideController rideController;

    private RideResponse sampleRideResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(rideController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        Ride ride = new Ride("passenger-123", "Colombo 03", "Kandy Central");
        ride.setId("ride-001");
        ride.setDriverId("driver-456");
        ride.setStatus(RideStatus.ASSIGNED);
        ride.setEstimatedFare(new BigDecimal("1500.00"));
        sampleRideResponse = new RideResponse(ride);
    }

    @Test
    @DisplayName("POST /api/rides - Should return 201 Created on valid ride creation")
    void createRide_Created() throws Exception {
        CreateRideRequest request = new CreateRideRequest();
        request.setPassengerId("passenger-123");
        request.setPickupLocation("Colombo 03");
        request.setDestination("Kandy Central");

        when(rideService.createRide(any(CreateRideRequest.class))).thenReturn(sampleRideResponse);

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rideId").value("ride-001"))
                .andExpect(jsonPath("$.passengerId").value("passenger-123"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    @DisplayName("POST /api/rides - Should return 400 Bad Request on invalid request body")
    void createRide_BadRequest() throws Exception {
        CreateRideRequest request = new CreateRideRequest();
        // missing required fields

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/rides/{rideId}/assign - Should assign driver")
    void assignDriver_Success() throws Exception {
        AssignDriverRequest request = new AssignDriverRequest();
        request.setDriverId("driver-456");

        when(rideService.assignDriver(eq("ride-001"), eq("driver-456"))).thenReturn(sampleRideResponse);

        mockMvc.perform(patch("/api/rides/ride-001/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId").value("driver-456"));
    }

    @Test
    @DisplayName("PATCH /api/rides/{rideId}/accept - Should accept ride")
    void acceptRide_Success() throws Exception {
        AcceptRideRequest request = new AcceptRideRequest();
        request.setDriverId("driver-456");

        when(rideService.acceptRide(eq("ride-001"), any(AcceptRideRequest.class))).thenReturn(sampleRideResponse);

        mockMvc.perform(patch("/api/rides/ride-001/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/rides/{rideId} - Should return ride details")
    void getRideById_Success() throws Exception {
        when(rideService.getRideById("ride-001")).thenReturn(sampleRideResponse);

        mockMvc.perform(get("/api/rides/ride-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value("ride-001"));
    }

    @Test
    @DisplayName("GET /api/rides/{rideId} - Should return 404 when ride not found")
    void getRideById_NotFound() throws Exception {
        when(rideService.getRideById("ride-001")).thenThrow(new RideNotFoundException("Ride not found: ride-001"));

        mockMvc.perform(get("/api/rides/ride-001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ride not found: ride-001"));
    }

    @Test
    @DisplayName("GET /api/rides/passenger/{passengerId} - Should return list of rides")
    void getRidesByPassenger_Success() throws Exception {
        when(rideService.getRidesByPassenger("passenger-123")).thenReturn(List.of(sampleRideResponse));

        mockMvc.perform(get("/api/rides/passenger/passenger-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passengerId").value("passenger-123"));
    }

    @Test
    @DisplayName("GET /api/rides/driver/{driverId} - Should return list of rides")
    void getRidesByDriver_Success() throws Exception {
        when(rideService.getRidesByDriver("driver-456")).thenReturn(List.of(sampleRideResponse));

        mockMvc.perform(get("/api/rides/driver/driver-456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].driverId").value("driver-456"));
    }
}
