package com.ridelink.ride_management_service.service;

import com.ridelink.ride_management_service.client.DriverServiceClient;
import com.ridelink.ride_management_service.client.FarePaymentClient;
import com.ridelink.ride_management_service.dto.CreateRideRequest;
import com.ridelink.ride_management_service.dto.RideResponse;
import com.ridelink.ride_management_service.enums.RideStatus;
import com.ridelink.ride_management_service.exception.InvalidRideTransitionException;
import com.ridelink.ride_management_service.exception.NoAvailableDriverException;
import com.ridelink.ride_management_service.exception.RideNotFoundException;
import com.ridelink.ride_management_service.model.Ride;
import com.ridelink.ride_management_service.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock private RideRepository rideRepository;
    @Mock private DriverServiceClient driverServiceClient;
    @Mock private FarePaymentClient farePaymentClient;

    @InjectMocks
    private RideService rideService;

    private Ride sampleRide;

    @BeforeEach
    void setUp() {
        sampleRide = Ride.builder()
                .rideId("ride-001")
                .passengerId("passenger-001")
                .pickup("Colombo")
                .destination("Kandy")
                .serviceArea("Colombo")
                .distanceKm(100.0)
                .status(RideStatus.REQUESTED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─── Create Ride Tests ───────────────────────────────────────────

    @Test
    void createRide_ValidRequest_ReturnsCreatedRide() {
        CreateRideRequest req = new CreateRideRequest();
        req.setPickup("Colombo");
        req.setDestination("Kandy");
        req.setServiceArea("Colombo");
        req.setDistanceKm(100.0);

        when(rideRepository.save(any(Ride.class))).thenReturn(sampleRide);

        RideResponse response = rideService.createRide("passenger-001", req);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(RideStatus.REQUESTED);
        assertThat(response.getPassengerId()).isEqualTo("passenger-001");
    }

    @Test
    void createRide_SamePickupDestination_ThrowsException() {
        CreateRideRequest req = new CreateRideRequest();
        req.setPickup("Colombo");
        req.setDestination("Colombo");
        req.setServiceArea("Colombo");
        req.setDistanceKm(0.0);

        assertThatThrownBy(() -> rideService.createRide("passenger-001", req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── Assign Driver Tests ─────────────────────────────────────────

    @Test
    void assignDriver_AvailableDriver_AssignsSuccessfully() {
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));
        // FIX: Driver service returns "accountId" (not "driverId") — matches RideService line 78
        when(driverServiceClient.getEligibleDrivers("Colombo"))
                .thenReturn(List.of(Map.of("accountId", "driver-001")));
        when(rideRepository.save(any())).thenReturn(
                sampleRide.toBuilder().driverId("driver-001").status(RideStatus.ASSIGNED).build());

        RideResponse response = rideService.assignDriver("ride-001", "passenger-001");

        assertThat(response.getStatus()).isEqualTo(RideStatus.ASSIGNED);
        assertThat(response.getDriverId()).isEqualTo("driver-001");
    }

    @Test
    void assignDriver_NoAvailableDriver_ThrowsNoAvailableDriverException() {
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));
        when(driverServiceClient.getEligibleDrivers("Colombo")).thenReturn(List.of());

        assertThatThrownBy(() -> rideService.assignDriver("ride-001", "passenger-001"))
                .isInstanceOf(NoAvailableDriverException.class);
    }

    @Test
    void assignDriver_DownstreamServiceReturns403_ThrowsIllegalStateException() {
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));
        
        org.springframework.web.client.HttpClientErrorException forbidden = 
            org.springframework.web.client.HttpClientErrorException.create(
                org.springframework.http.HttpStatus.FORBIDDEN, 
                "Forbidden", 
                org.springframework.http.HttpHeaders.EMPTY, 
                null, null);

        when(driverServiceClient.getEligibleDrivers("Colombo")).thenThrow(forbidden);

        assertThatThrownBy(() -> rideService.assignDriver("ride-001", "passenger-001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Downstream configuration or access error: 403 FORBIDDEN");
    }

    @Test
    void assignDriver_DownstreamServiceConnectionError_ThrowsResourceAccessException() {
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));
        
        org.springframework.web.client.ResourceAccessException connectionError = 
            new org.springframework.web.client.ResourceAccessException("Connection refused");

        when(driverServiceClient.getEligibleDrivers("Colombo")).thenThrow(connectionError);

        assertThatThrownBy(() -> rideService.assignDriver("ride-001", "passenger-001"))
                .isInstanceOf(org.springframework.web.client.ResourceAccessException.class)
                .hasMessageContaining("Connection refused");
    }

    @Test
    void assignDriver_AlreadyAssignedRide_ThrowsInvalidTransition() {
        sampleRide.setStatus(RideStatus.ASSIGNED);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));

        assertThatThrownBy(() -> rideService.assignDriver("ride-001", "passenger-001"))
                .isInstanceOf(InvalidRideTransitionException.class);
    }

    // ─── Accept Ride Tests ───────────────────────────────────────────

    @Test
    void acceptRide_ValidDriver_AssignedRide_Success() {
        sampleRide.setStatus(RideStatus.ASSIGNED);
        sampleRide.setDriverId("driver-001");
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));
        when(rideRepository.save(any())).thenReturn(
                sampleRide.toBuilder()
                        .status(RideStatus.ACCEPTED)
                        .acceptedAt(LocalDateTime.now())
                        .build());

        RideResponse response = rideService.acceptRide("ride-001", "driver-001");

        assertThat(response.getStatus()).isEqualTo(RideStatus.ACCEPTED);
        assertThat(response.getAcceptedAt()).isNotNull();
    }

    @Test
    void acceptRide_WrongDriver_ThrowsAccessDenied() {
        sampleRide.setStatus(RideStatus.ASSIGNED);
        sampleRide.setDriverId("driver-001");
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));

        assertThatThrownBy(() -> rideService.acceptRide("ride-001", "wrong-driver"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void acceptRide_FromRequestedState_ThrowsInvalidTransition() {
        // status is REQUESTED (not ASSIGNED)
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));

        assertThatThrownBy(() -> rideService.acceptRide("ride-001", "driver-001"))
                .isInstanceOf(InvalidRideTransitionException.class);
    }

    // ─── Start Ride Tests ─────────────────────────────────────────────

    @Test
    void startRide_AcceptedRide_CorrectDriver_Success() {
        sampleRide.setStatus(RideStatus.ACCEPTED);
        sampleRide.setDriverId("driver-001");
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));
        when(rideRepository.save(any())).thenReturn(
                sampleRide.toBuilder()
                        .status(RideStatus.IN_PROGRESS)
                        .startedAt(LocalDateTime.now())
                        .build());

        RideResponse response = rideService.startRide("ride-001", "driver-001");

        assertThat(response.getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
        assertThat(response.getStartedAt()).isNotNull();
    }

    @Test
    void startRide_WrongDriver_ThrowsAccessDenied() {
        sampleRide.setStatus(RideStatus.ACCEPTED);
        sampleRide.setDriverId("driver-001");
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));

        assertThatThrownBy(() -> rideService.startRide("ride-001", "wrong-driver"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ─── Complete Ride Tests ──────────────────────────────────────────

    @Test
    void completeRide_InProgress_FareServiceDown_StillCompletes() {
        sampleRide.setStatus(RideStatus.IN_PROGRESS);
        sampleRide.setDriverId("driver-001");
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));
        when(rideRepository.save(any())).thenReturn(
                sampleRide.toBuilder()
                        .status(RideStatus.COMPLETED)
                        .completedAt(LocalDateTime.now())
                        .build());
        when(farePaymentClient.createFinalFare(anyString(), anyDouble()))
                .thenThrow(new RuntimeException("Fare service unavailable"));

        // Should NOT throw — graceful degradation
        RideResponse response = rideService.completeRide("ride-001", "driver-001");

        assertThat(response.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(response.getFinalFareId()).isNull();
    }

    @Test
    void completeRide_WrongDriver_ThrowsAccessDenied() {
        sampleRide.setStatus(RideStatus.IN_PROGRESS);
        sampleRide.setDriverId("driver-001");
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));

        assertThatThrownBy(() -> rideService.completeRide("ride-001", "wrong-driver"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ─── Cancel Ride Tests ───────────────────────────────────────────

    @Test
    void cancelRide_ByPassengerOwner_Success() {
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));
        when(rideRepository.save(any())).thenReturn(
                sampleRide.toBuilder().status(RideStatus.CANCELLED).cancelledAt(LocalDateTime.now()).build());

        RideResponse response = rideService.cancelRide("ride-001", "passenger-001");

        assertThat(response.getStatus()).isEqualTo(RideStatus.CANCELLED);
    }

    @Test
    void cancelRide_ByAssignedDriver_Success() {
        sampleRide.setStatus(RideStatus.ASSIGNED);
        sampleRide.setDriverId("driver-001");
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));
        when(rideRepository.save(any())).thenReturn(
                sampleRide.toBuilder().status(RideStatus.CANCELLED).cancelledAt(LocalDateTime.now()).build());

        RideResponse response = rideService.cancelRide("ride-001", "driver-001");

        assertThat(response.getStatus()).isEqualTo(RideStatus.CANCELLED);
    }

    @Test
    void cancelRide_CompletedRide_ThrowsInvalidTransition() {
        sampleRide.setStatus(RideStatus.COMPLETED);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));

        assertThatThrownBy(() -> rideService.cancelRide("ride-001", "passenger-001"))
                .isInstanceOf(InvalidRideTransitionException.class);
    }

    @Test
    void cancelRide_UnrelatedCaller_ThrowsAccessDenied() {
        sampleRide.setDriverId("driver-001");
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));

        assertThatThrownBy(() -> rideService.cancelRide("ride-001", "unrelated-user"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ─── Get Ride Tests ──────────────────────────────────────────────

    @Test
    void getRideById_UnknownId_ThrowsRideNotFound() {
        when(rideRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.getRideById("unknown", "passenger-001"))
                .isInstanceOf(RideNotFoundException.class);
    }

    @Test
    void getRideById_UnauthorizedUser_ThrowsAccessDenied() {
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));

        assertThatThrownBy(() -> rideService.getRideById("ride-001", "other-user"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ─── Get Rides by Passenger Tests ─────────────────────────────────

    @Test
    void getRidesByPassenger_OwnRides_ReturnsCorrectList() {
        when(rideRepository.findByPassengerId("passenger-001"))
                .thenReturn(List.of(sampleRide));

        List<RideResponse> rides = rideService.getRidesByPassenger("passenger-001", "passenger-001");

        assertThat(rides).hasSize(1);
        assertThat(rides.get(0).getPassengerId()).isEqualTo("passenger-001");
        assertThat(rides.get(0).getStatus()).isEqualTo(RideStatus.REQUESTED);
    }

    @Test
    void getRidesByPassenger_OtherUserCalling_ThrowsAccessDenied() {
        assertThatThrownBy(() -> rideService.getRidesByPassenger("passenger-001", "other-user"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ─── Get Rides by Driver Tests ─────────────────────────────────────

    @Test
    void getRidesByDriver_OwnRides_ReturnsCorrectList() {
        sampleRide.setStatus(RideStatus.ASSIGNED);
        sampleRide.setDriverId("driver-001");
        when(rideRepository.findByDriverId("driver-001")).thenReturn(List.of(sampleRide));

        List<RideResponse> rides = rideService.getRidesByDriver("driver-001", "driver-001");

        assertThat(rides).hasSize(1);
        assertThat(rides.get(0).getDriverId()).isEqualTo("driver-001");
        assertThat(rides.get(0).getStatus()).isEqualTo(RideStatus.ASSIGNED);
    }

    @Test
    void getRidesByDriver_OtherUserCalling_ThrowsAccessDenied() {
        assertThatThrownBy(() -> rideService.getRidesByDriver("driver-001", "other-user"))
                .isInstanceOf(AccessDeniedException.class);
    }
}
