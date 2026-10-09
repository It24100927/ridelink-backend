package com.ridelink.ride_management_service.service;

import com.ridelink.ride_management_service.client.AccountServiceClient;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final DriverServiceClient driverServiceClient;
    private final FarePaymentClient farePaymentClient;
    private final AccountServiceClient accountServiceClient;

    // Create Ride Request
    public RideResponse createRide(String passengerId, CreateRideRequest request) {
        if (request.getPickup().equalsIgnoreCase(request.getDestination())) {
            throw new IllegalArgumentException("Pickup and destination cannot be the same");
        }

        Ride ride = Ride.builder()
                .rideId(UUID.randomUUID().toString())
                .passengerId(passengerId)
                .pickup(request.getPickup())
                .destination(request.getDestination())
                .serviceArea(request.getServiceArea())
                .distanceKm(request.getDistanceKm())
                .status(RideStatus.REQUESTED)
                .createdAt(LocalDateTime.now())
                .build();

        Ride saved = rideRepository.save(ride);
        log.info("Ride created: {} by passenger: {}", saved.getRideId(), passengerId);
        return mapToResponse(saved);
    }

    // Assign Driver
    public RideResponse assignDriver(String rideId, String requesterId) {
        Ride ride = getRideOrThrow(rideId);

        // Only REQUESTED rides can be assigned
        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new InvalidRideTransitionException(ride.getStatus(), RideStatus.ASSIGNED);
        }

        // Call Driver & Vehicle Service
        List<Map<String, Object>> eligibleDrivers;
        try {
            eligibleDrivers = driverServiceClient.getEligibleDrivers(ride.getServiceArea());
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("Driver service returned HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("Downstream configuration or access error: " + e.getStatusCode());
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Driver service unavailable (connection error): {}", e.getMessage());
            throw e;
        }

        if (eligibleDrivers == null || eligibleDrivers.isEmpty()) {
            throw new NoAvailableDriverException(ride.getServiceArea());
        }

        // First eligible driver select (documented simple rule)
        // NOTE: Driver service returns "accountId" (not "userId") as the driver reference
        String selectedDriverId = (String) eligibleDrivers.get(0).get("accountId");

        ride.setDriverId(selectedDriverId);
        ride.setStatus(RideStatus.ASSIGNED);
        ride.setAssignedAt(LocalDateTime.now());
        Ride saved = rideRepository.save(ride);
        log.info("Driver {} assigned to ride {}", selectedDriverId, rideId);
        return mapToResponse(saved);
    }

    // Driver Accepts Ride
    public RideResponse acceptRide(String rideId, String driverId) {
        Ride ride = getRideOrThrow(rideId);

        if (ride.getStatus() != RideStatus.ASSIGNED) {
            throw new InvalidRideTransitionException(ride.getStatus(), RideStatus.ACCEPTED);
        }

        if (!driverId.equals(ride.getDriverId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only the assigned driver can accept this ride");
        }

        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(LocalDateTime.now());
        Ride saved = rideRepository.save(ride);
        log.info("Ride {} accepted by driver {}", rideId, driverId);
        return mapToResponse(saved);
    }

    // Start Ride
    public RideResponse startRide(String rideId, String driverId) {
        Ride ride = getRideOrThrow(rideId);

        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new InvalidRideTransitionException(ride.getStatus(), RideStatus.IN_PROGRESS);
        }

        if (!driverId.equals(ride.getDriverId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only the assigned driver can start this ride");
        }

        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(LocalDateTime.now());
        Ride saved = rideRepository.save(ride);
        log.info("Ride {} started by driver {}", rideId, driverId);
        return mapToResponse(saved);
    }

    // Complete Ride + Final Fare
    public RideResponse completeRide(String rideId, String driverId) {
        Ride ride = getRideOrThrow(rideId);

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new InvalidRideTransitionException(ride.getStatus(), RideStatus.COMPLETED);
        }

        if (!driverId.equals(ride.getDriverId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only the assigned driver can complete this ride");
        }

        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());
        Ride saved = rideRepository.save(ride);

        // Call Fare & Payment Service - final fare create
        try {
            String fareId = farePaymentClient.createFinalFare(rideId, ride.getDistanceKm());
            if (fareId != null) {
                saved.setFinalFareId(fareId);
                saved = rideRepository.save(saved);
            }
        } catch (Exception e) {
            // Fare service unavailable - ride stays COMPLETED, fare pending
            log.warn("Fare service unavailable for ride {}: {}", rideId, e.getMessage());
            // Controlled failure - don't rollback ride completion
        }

        log.info("Ride {} completed. FinalFareId: {}", rideId, saved.getFinalFareId());
        return mapToResponse(saved);
    }

    // Cancel Ride
    public RideResponse cancelRide(String rideId, String callerId) {
        Ride ride = getRideOrThrow(rideId);

        // Cannot cancel COMPLETED or already CANCELLED
        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            throw new InvalidRideTransitionException(ride.getStatus(), RideStatus.CANCELLED);
        }

        // Only passenger owner OR assigned driver can cancel
        boolean isPassenger = callerId.equals(ride.getPassengerId());
        boolean isAssignedDriver = callerId.equals(ride.getDriverId());

        if (!isPassenger && !isAssignedDriver) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only the passenger or assigned driver can cancel this ride");
        }

        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancelledAt(LocalDateTime.now());
        Ride saved = rideRepository.save(ride);
        log.info("Ride {} cancelled by {}", rideId, callerId);
        return mapToResponse(saved);
    }

    // Retrieve Rides
    public RideResponse getRideById(String rideId, String callerId) {
        Ride ride = getRideOrThrow(rideId);

        // Only passenger or assigned driver can view
        if (!callerId.equals(ride.getPassengerId()) && !callerId.equals(ride.getDriverId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not authorized to view this ride");
        }

        return mapToResponse(ride);
    }

    public List<RideResponse> getRidesByPassenger(String passengerId, String callerId) {
        // Caller must be the passenger themselves
        if (!callerId.equals(passengerId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only view your own rides");
        }
        return rideRepository.findByPassengerId(passengerId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<RideResponse> getRidesByDriver(String driverId, String callerId) {
        // Caller must be the driver themselves
        if (!callerId.equals(driverId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only view your own rides");
        }
        return rideRepository.findByDriverId(driverId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // Helper Methods
    private Ride getRideOrThrow(String rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId));
    }

    private RideResponse mapToResponse(Ride ride) {
        return RideResponse.builder()
                .rideId(ride.getRideId())
                .passengerId(ride.getPassengerId())
                .driverId(ride.getDriverId())
                .pickup(ride.getPickup())
                .destination(ride.getDestination())
                .serviceArea(ride.getServiceArea())
                .distanceKm(ride.getDistanceKm())
                .status(ride.getStatus())
                .finalFareId(ride.getFinalFareId())
                .createdAt(ride.getCreatedAt())
                .assignedAt(ride.getAssignedAt())
                .acceptedAt(ride.getAcceptedAt())
                .startedAt(ride.getStartedAt())
                .completedAt(ride.getCompletedAt())
                .cancelledAt(ride.getCancelledAt())
                .build();
    }
}
