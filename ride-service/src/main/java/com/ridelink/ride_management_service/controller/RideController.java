package com.ridelink.ride_management_service.controller;

import com.ridelink.ride_management_service.dto.CreateRideRequest;
import com.ridelink.ride_management_service.dto.RideResponse;
import com.ridelink.ride_management_service.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
@Tag(name = "Ride Management", description = "Ride lifecycle operations")
@SecurityRequirement(name = "bearerAuth")
public class RideController {

    private final RideService rideService;

    // POST /api/rides - Create ride
    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    @Operation(summary = "Create a new ride request")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Ride created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or pickup equals destination"),
        @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required"),
        @ApiResponse(responseCode = "403", description = "Forbidden — PASSENGER role required")
    })
    public ResponseEntity<RideResponse> createRide(
            @Valid @RequestBody CreateRideRequest request,
            Authentication auth) {
        String passengerId = auth.getName();
        RideResponse response = rideService.createRide(passengerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // POST /api/rides/{rideId}/assign - Assign driver
    @PostMapping("/{rideId}/assign")
    @PreAuthorize("hasRole('PASSENGER')")
    @Operation(summary = "Assign an available driver to the ride")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Driver assigned successfully"),
        @ApiResponse(responseCode = "404", description = "Ride not found"),
        @ApiResponse(responseCode = "409", description = "Invalid status transition or no available driver"),
        @ApiResponse(responseCode = "503", description = "Driver service unavailable")
    })
    public ResponseEntity<RideResponse> assignDriver(
            @PathVariable String rideId,
            Authentication auth) {
        RideResponse response = rideService.assignDriver(rideId, auth.getName());
        return ResponseEntity.ok(response);
    }

    // POST /api/rides/{rideId}/accept - Accept ride
    @PostMapping("/{rideId}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Assigned driver accepts the ride")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ride accepted successfully"),
        @ApiResponse(responseCode = "403", description = "Caller is not the assigned driver"),
        @ApiResponse(responseCode = "404", description = "Ride not found"),
        @ApiResponse(responseCode = "409", description = "Ride is not in ASSIGNED state")
    })
    public ResponseEntity<RideResponse> acceptRide(
            @PathVariable String rideId,
            Authentication auth) {
        RideResponse response = rideService.acceptRide(rideId, auth.getName());
        return ResponseEntity.ok(response);
    }

    // POST /api/rides/{rideId}/start - Start ride
    @PostMapping("/{rideId}/start")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Driver starts the ride (ACCEPTED → IN_PROGRESS)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ride started successfully"),
        @ApiResponse(responseCode = "403", description = "Caller is not the assigned driver"),
        @ApiResponse(responseCode = "404", description = "Ride not found"),
        @ApiResponse(responseCode = "409", description = "Ride is not in ACCEPTED state")
    })
    public ResponseEntity<RideResponse> startRide(
            @PathVariable String rideId,
            Authentication auth) {
        RideResponse response = rideService.startRide(rideId, auth.getName());
        return ResponseEntity.ok(response);
    }

    // POST /api/rides/{rideId}/complete - Complete ride
    @PostMapping("/{rideId}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Driver completes the ride and triggers final fare")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ride completed; final fare triggered"),
        @ApiResponse(responseCode = "403", description = "Caller is not the assigned driver"),
        @ApiResponse(responseCode = "404", description = "Ride not found"),
        @ApiResponse(responseCode = "409", description = "Ride is not IN_PROGRESS")
    })
    public ResponseEntity<RideResponse> completeRide(
            @PathVariable String rideId,
            Authentication auth) {
        RideResponse response = rideService.completeRide(rideId, auth.getName());
        return ResponseEntity.ok(response);
    }

    // POST /api/rides/{rideId}/cancel - Cancel ride
    @PostMapping("/{rideId}/cancel")
    @PreAuthorize("isAuthenticated()") // Both PASSENGER (ride owner) and DRIVER (assigned) may cancel
    @Operation(summary = "Cancel a ride (passenger or assigned driver)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ride cancelled successfully"),
        @ApiResponse(responseCode = "403", description = "Caller is neither the passenger nor the assigned driver"),
        @ApiResponse(responseCode = "404", description = "Ride not found"),
        @ApiResponse(responseCode = "409", description = "Ride is COMPLETED or already CANCELLED")
    })
    public ResponseEntity<RideResponse> cancelRide(
            @PathVariable String rideId,
            Authentication auth) {
        RideResponse response = rideService.cancelRide(rideId, auth.getName());
        return ResponseEntity.ok(response);
    }

    // GET /api/rides/{rideId} - Get single ride
    @GetMapping("/{rideId}")
    @Operation(summary = "Get ride details by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ride details returned"),
        @ApiResponse(responseCode = "403", description = "Caller is not the passenger or driver of this ride"),
        @ApiResponse(responseCode = "404", description = "Ride not found")
    })
    public ResponseEntity<RideResponse> getRide(
            @PathVariable String rideId,
            Authentication auth) {
        RideResponse response = rideService.getRideById(rideId, auth.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/rides?passengerId=X  — retrieves all rides for a passenger
     * GET /api/rides?driverId=X     — retrieves all rides assigned to a driver
     * Exactly one parameter must be supplied.
     */
    @GetMapping
    @Operation(summary = "Get all rides filtered by passengerId or driverId")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rides list returned"),
        @ApiResponse(responseCode = "400", description = "Neither passengerId nor driverId was provided"),
        @ApiResponse(responseCode = "403", description = "Caller can only query their own rides")
    })
    public ResponseEntity<List<RideResponse>> getRides(
            @RequestParam(required = false) String passengerId,
            @RequestParam(required = false) String driverId,
            Authentication auth) {
        if (passengerId != null) {
            return ResponseEntity.ok(rideService.getRidesByPassenger(passengerId, auth.getName()));
        } else if (driverId != null) {
            return ResponseEntity.ok(rideService.getRidesByDriver(driverId, auth.getName()));
        } else {
            throw new IllegalArgumentException("Either passengerId or driverId query parameter is required");
        }
    }
}
