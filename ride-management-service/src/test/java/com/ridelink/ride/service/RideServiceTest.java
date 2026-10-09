package com.ridelink.ride.service;

import com.ridelink.ride.client.DriverClient;
import com.ridelink.ride.client.FareClient;
import com.ridelink.ride.dto.AcceptRideRequest;
import com.ridelink.ride.dto.AvailableDriverResponse;
import com.ridelink.ride.dto.CancelRideRequest;
import com.ridelink.ride.dto.CompleteRideRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.FareEstimateRequest;
import com.ridelink.ride.dto.FareEstimateResponse;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.dto.StartRideRequest;
import com.ridelink.ride.exception.DriverNotAssignedException;
import com.ridelink.ride.exception.InvalidRideStateException;
import com.ridelink.ride.exception.NoAvailableDriverException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private DriverClient driverClient;

    @Mock
    private FareClient fareClient;

    @InjectMocks
    private RideService rideService;

    private CreateRideRequest createRideRequest;
    private AvailableDriverResponse driverResponse;
    private FareEstimateResponse fareEstimateResponse;
    private Ride sampleRide;

    @BeforeEach
    void setUp() {
        createRideRequest = new CreateRideRequest();
        createRideRequest.setPassengerId("passenger-123");
        createRideRequest.setPickupLocation("Colombo 03");
        createRideRequest.setDestination("Kandy Central");

        driverResponse = new AvailableDriverResponse();
        driverResponse.setDriverId("driver-456");
        driverResponse.setName("John Doe");
        driverResponse.setAvailability("AVAILABLE");

        fareEstimateResponse = new FareEstimateResponse();
        fareEstimateResponse.setEstimatedFare(new BigDecimal("1500.00"));

        sampleRide = new Ride("passenger-123", "Colombo 03", "Kandy Central");
        sampleRide.setId("ride-001");
        sampleRide.setStatus(RideStatus.REQUESTED);
        sampleRide.setEstimatedFare(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("Should successfully create a ride and assign available driver")
    void createRide_Success() {
        when(fareClient.estimateFare(any(FareEstimateRequest.class))).thenReturn(fareEstimateResponse);
        when(driverClient.getEligibleDrivers()).thenReturn(List.of(driverResponse));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride r = invocation.getArgument(0);
            r.setId("ride-001");
            return r;
        });

        RideResponse response = rideService.createRide(createRideRequest);

        assertNotNull(response);
        assertEquals("passenger-123", response.getPassengerId());
        assertEquals("driver-456", response.getDriverId());
        assertEquals(RideStatus.ASSIGNED, response.getStatus());
        assertEquals(new BigDecimal("1500.00"), response.getEstimatedFare());

        verify(fareClient, times(1)).estimateFare(any(FareEstimateRequest.class));
        verify(driverClient, times(1)).getEligibleDrivers();
        verify(rideRepository, times(1)).save(any(Ride.class));
    }

    @Test
    @DisplayName("Should throw NoAvailableDriverException when no driver is available on creation")
    void createRide_NoAvailableDriver() {
        when(fareClient.estimateFare(any(FareEstimateRequest.class))).thenReturn(fareEstimateResponse);
        when(driverClient.getEligibleDrivers()).thenReturn(List.of());
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(NoAvailableDriverException.class, () -> rideService.createRide(createRideRequest));

        verify(rideRepository, times(1)).save(any(Ride.class));
    }

    @Test
    @DisplayName("Should successfully assign specified driver to requested ride")
    void assignDriver_Success() {
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));
        when(driverClient.getEligibleDrivers()).thenReturn(List.of(driverResponse));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RideResponse response = rideService.assignDriver("ride-001", "driver-456");

        assertNotNull(response);
        assertEquals("driver-456", response.getDriverId());
        assertEquals(RideStatus.ASSIGNED, response.getStatus());
    }

    @Test
    @DisplayName("Should throw InvalidRideStateException when assigning driver to non-requested ride")
    void assignDriver_InvalidState() {
        sampleRide.setStatus(RideStatus.IN_PROGRESS);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));

        assertThrows(InvalidRideStateException.class, () -> rideService.assignDriver("ride-001", "driver-456"));
    }

    @Test
    @DisplayName("Should successfully accept ride by assigned driver")
    void acceptRide_Success() {
        sampleRide.setStatus(RideStatus.ASSIGNED);
        sampleRide.setDriverId("driver-456");
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AcceptRideRequest request = new AcceptRideRequest();
        request.setDriverId("driver-456");

        RideResponse response = rideService.acceptRide("ride-001", request);

        assertEquals(RideStatus.ACCEPTED, response.getStatus());
    }

    @Test
    @DisplayName("Should throw DriverNotAssignedException when unassigned driver tries to accept")
    void acceptRide_WrongDriver() {
        sampleRide.setStatus(RideStatus.ASSIGNED);
        sampleRide.setDriverId("driver-456");
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));

        AcceptRideRequest request = new AcceptRideRequest();
        request.setDriverId("driver-999");

        assertThrows(DriverNotAssignedException.class, () -> rideService.acceptRide("ride-001", request));
    }

    @Test
    @DisplayName("Should successfully start accepted ride")
    void startRide_Success() {
        sampleRide.setStatus(RideStatus.ACCEPTED);
        sampleRide.setDriverId("driver-456");
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StartRideRequest request = new StartRideRequest();
        request.setDriverId("driver-456");

        RideResponse response = rideService.startRide("ride-001", request);

        assertEquals(RideStatus.IN_PROGRESS, response.getStatus());
    }

    @Test
    @DisplayName("Should successfully complete in-progress ride")
    void completeRide_Success() {
        sampleRide.setStatus(RideStatus.IN_PROGRESS);
        sampleRide.setDriverId("driver-456");
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompleteRideRequest request = new CompleteRideRequest();
        request.setDriverId("driver-456");

        RideResponse response = rideService.completeRide("ride-001", request);

        assertEquals(RideStatus.COMPLETED, response.getStatus());
    }

    @Test
    @DisplayName("Should successfully cancel requested ride by creator passenger")
    void cancelRide_Success() {
        sampleRide.setStatus(RideStatus.REQUESTED);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(sampleRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CancelRideRequest request = new CancelRideRequest();
        request.setPassengerId("passenger-123");

        RideResponse response = rideService.cancelRide("ride-001", request);

        assertEquals(RideStatus.CANCELLED, response.getStatus());
    }

    @Test
    @DisplayName("Should throw RideNotFoundException when ride ID does not exist")
    void getRideById_NotFound() {
        when(rideRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(RideNotFoundException.class, () -> rideService.getRideById("non-existent"));
    }

    @Test
    @DisplayName("Should retrieve rides by passenger ID")
    void getRidesByPassenger_Success() {
        when(rideRepository.findByPassengerId("passenger-123")).thenReturn(List.of(sampleRide));

        List<RideResponse> responses = rideService.getRidesByPassenger("passenger-123");

        assertEquals(1, responses.size());
        assertEquals("passenger-123", responses.get(0).getPassengerId());
    }

    @Test
    @DisplayName("Should retrieve rides by driver ID")
    void getRidesByDriver_Success() {
        sampleRide.setDriverId("driver-456");
        when(rideRepository.findByDriverId("driver-456")).thenReturn(List.of(sampleRide));

        List<RideResponse> responses = rideService.getRidesByDriver("driver-456");

        assertEquals(1, responses.size());
        assertEquals("driver-456", responses.get(0).getDriverId());
    }
}
