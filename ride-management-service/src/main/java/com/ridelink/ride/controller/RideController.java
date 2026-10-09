package com.ridelink.ride.controller;

import com.ridelink.ride.dto.AcceptRideRequest;
import com.ridelink.ride.dto.AssignDriverRequest;
import com.ridelink.ride.dto.CancelRideRequest;
import com.ridelink.ride.dto.CompleteRideRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.ErrorResponse;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.dto.StartRideRequest;
import com.ridelink.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rides")
@Tag(name = "Ride Management API", description = "Endpoints for ride requests, driver assignment, status transitions, and ride retrieval")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    @Operation(summary = "Create a new ride request", description = "Requests a ride, estimates fare with Fare Service, and automatically assigns an available driver if present.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ride created and assigned successfully",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "No available driver found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RideResponse> createRide(
            @Valid @RequestBody CreateRideRequest request) {

        RideResponse response = rideService.createRide(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{rideId}/assign")
    @Operation(summary = "Assign a specific driver to a ride", description = "Assigns an eligible driver to a requested ride.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Driver assigned successfully",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ride not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ride not in REQUESTED state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RideResponse> assignDriver(
            @Parameter(description = "ID of the ride to assign") @PathVariable String rideId,
            @Valid @RequestBody AssignDriverRequest request) {

        RideResponse response = rideService.assignDriver(
                rideId,
                request.getDriverId());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{rideId}/accept")
    @Operation(summary = "Accept an assigned ride", description = "Driver accepts an assigned ride.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ride accepted successfully",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "400", description = "Driver not assigned to this ride",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ride not in ASSIGNED state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RideResponse> acceptRide(
            @Parameter(description = "ID of the ride to accept") @PathVariable String rideId,
            @Valid @RequestBody AcceptRideRequest request) {

        RideResponse response = rideService.acceptRide(
                rideId,
                request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{rideId}/start")
    @Operation(summary = "Start an accepted ride", description = "Driver marks the ride as IN_PROGRESS.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ride started successfully",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ride not in ACCEPTED state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RideResponse> startRide(
            @Parameter(description = "ID of the ride to start") @PathVariable String rideId,
            @Valid @RequestBody StartRideRequest request) {

        RideResponse response = rideService.startRide(
                rideId,
                request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{rideId}/complete")
    @Operation(summary = "Complete an in-progress ride", description = "Driver marks the ride as COMPLETED.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ride completed successfully",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ride not in IN_PROGRESS state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RideResponse> completeRide(
            @Parameter(description = "ID of the ride to complete") @PathVariable String rideId,
            @Valid @RequestBody CompleteRideRequest request) {

        RideResponse response = rideService.completeRide(
                rideId,
                request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{rideId}/cancel")
    @Operation(summary = "Cancel a ride request", description = "Passenger cancels the ride before it starts.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ride cancelled successfully",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ride cannot be cancelled in current state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RideResponse> cancelRide(
            @Parameter(description = "ID of the ride to cancel") @PathVariable String rideId,
            @Valid @RequestBody CancelRideRequest request) {

        RideResponse response = rideService.cancelRide(
                rideId,
                request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{rideId}")
    @Operation(summary = "Get ride by ID", description = "Retrieves details of a specific ride.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ride details retrieved",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ride not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RideResponse> getRideById(
            @Parameter(description = "ID of the ride to retrieve") @PathVariable String rideId) {

        RideResponse response = rideService.getRideById(rideId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/passenger/{passengerId}")
    @Operation(summary = "Get rides by passenger ID", description = "Retrieves all rides created by a specific passenger.")
    @ApiResponse(responseCode = "200", description = "List of passenger rides",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RideResponse.class))))
    public ResponseEntity<List<RideResponse>> getRidesByPassenger(
            @Parameter(description = "Passenger ID") @PathVariable String passengerId) {

        List<RideResponse> rides = rideService.getRidesByPassenger(passengerId);

        return ResponseEntity.ok(rides);
    }

    @GetMapping("/driver/{driverId}")
    @Operation(summary = "Get rides by driver ID", description = "Retrieves all rides assigned to a specific driver.")
    @ApiResponse(responseCode = "200", description = "List of driver rides",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RideResponse.class))))
    public ResponseEntity<List<RideResponse>> getRidesByDriver(
            @Parameter(description = "Driver ID") @PathVariable String driverId) {

        List<RideResponse> rides = rideService.getRidesByDriver(driverId);

        return ResponseEntity.ok(rides);
    }

}
