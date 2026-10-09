package com.ridelink.driver_vehicle_service.controller;

import com.ridelink.driver_vehicle_service.dto.request.CreateVehicleRequest;
import com.ridelink.driver_vehicle_service.dto.request.UpdateVehicleRequest;
import com.ridelink.driver_vehicle_service.dto.response.ErrorResponse;
import com.ridelink.driver_vehicle_service.dto.response.VehicleResponse;
import com.ridelink.driver_vehicle_service.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for all Vehicle endpoints.
 *
 * Both endpoints require the DRIVER role.
 * accountId is ALWAYS read from Authentication.getPrincipal() (set by JwtAuthFilter),
 * never from the request body.
 * driverId is NEVER accepted from the client — it is derived server-side in the service layer.
 */
@RestController
@RequestMapping("/api/vehicles")
@Tag(name = "Vehicles", description = "Vehicle registration and management")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    // -----------------------------------------------------------------------
    // POST /api/vehicles
    // -----------------------------------------------------------------------

    @Operation(
            summary = "Register a vehicle",
            description = "Registers a new vehicle for the authenticated driver. "
                        + "Requires DRIVER role. driverId is derived server-side from the JWT — "
                        + "do not include it in the request body. "
                        + "The driver must have an existing profile (POST /api/drivers) before "
                        + "registering a vehicle; otherwise 404 is returned.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vehicle registered",
                    content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Blank vehicleNumber, vehicleType, or model",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have DRIVER role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No driver profile found for this account",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "vehicleNumber already registered",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<VehicleResponse> createVehicle(
            @Valid @RequestBody CreateVehicleRequest request,
            Authentication auth) {

        String accountId = (String) auth.getPrincipal();
        VehicleResponse response = vehicleService.createVehicle(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -----------------------------------------------------------------------
    // PUT /api/vehicles/{vehicleId}
    // -----------------------------------------------------------------------

    @Operation(
            summary = "Update a vehicle",
            description = "Replaces vehicleNumber, vehicleType, and model for an existing vehicle. "
                        + "Requires DRIVER role and ownership of the vehicle.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle updated",
                    content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Blank vehicleNumber, vehicleType, or model",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not the vehicle owner or wrong role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{vehicleId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable String vehicleId,
            @Valid @RequestBody UpdateVehicleRequest request,
            Authentication auth) {

        String accountId = (String) auth.getPrincipal();
        return ResponseEntity.ok(vehicleService.updateVehicle(vehicleId, accountId, request));
    }
}
