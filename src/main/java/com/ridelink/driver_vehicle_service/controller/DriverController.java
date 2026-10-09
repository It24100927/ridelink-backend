package com.ridelink.driver_vehicle_service.controller;

import com.ridelink.driver_vehicle_service.dto.request.CreateDriverRequest;
import com.ridelink.driver_vehicle_service.dto.request.UpdateAvailabilityRequest;
import com.ridelink.driver_vehicle_service.dto.request.UpdateLocationRequest;
import com.ridelink.driver_vehicle_service.dto.request.UpdateServiceAreaRequest;
import com.ridelink.driver_vehicle_service.dto.response.AvailabilityResponse;
import com.ridelink.driver_vehicle_service.dto.response.DriverResponse;
import com.ridelink.driver_vehicle_service.dto.response.EligibleDriverResponse;
import com.ridelink.driver_vehicle_service.dto.response.ErrorResponse;
import com.ridelink.driver_vehicle_service.dto.response.LocationResponse;
import com.ridelink.driver_vehicle_service.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.List;

/**
 * REST controller for all Driver endpoints.
 *
 * Role enforcement strategy:
 *   - DRIVER-only endpoints are annotated with @PreAuthorize("hasRole('DRIVER')").
 *   - "any authenticated user" endpoints rely on the blanket .anyRequest().authenticated()
 *     rule in SecurityConfig — no further annotation needed.
 *
 * accountId is ALWAYS read from Authentication.getPrincipal() (set by JwtAuthFilter),
 * never from the request body.
 */
@RestController
@RequestMapping("/api/drivers")
@Tag(name = "Drivers", description = "Driver profile management")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    // -----------------------------------------------------------------------
    // POST /api/drivers
    // -----------------------------------------------------------------------

    @Operation(
            summary = "Register a driver profile",
            description = "Creates a new driver profile. Requires DRIVER role. "
                        + "accountId is read from the JWT subject — do not send it in the body.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Driver profile created",
                    content = @Content(schema = @Schema(implementation = DriverResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure (blank serviceArea or invalid coordinates)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have DRIVER role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "A driver profile already exists for this account",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverResponse> createDriver(
            @Valid @RequestBody CreateDriverRequest request,
            Authentication auth) {

        String accountId = (String) auth.getPrincipal();
        DriverResponse response = driverService.createDriver(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -----------------------------------------------------------------------
    // GET /api/drivers/eligible?serviceArea=X
    // NOTE: This mapping MUST be declared before /{driverId} so Spring MVC
    //       matches the literal segment "eligible" ahead of the path variable.
    // -----------------------------------------------------------------------

    @Operation(
            summary = "Find eligible (AVAILABLE) drivers by service area",
            description = "Returns all drivers with availability=AVAILABLE in the given service area. "
                        + "Returns an empty array when none found — never 404. "
                        + "Also called internally by the Ride Service.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of eligible drivers (may be empty)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EligibleDriverResponse.class)))),
            @ApiResponse(responseCode = "400", description = "serviceArea query parameter is missing or blank",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/eligible")
    public ResponseEntity<List<EligibleDriverResponse>> getEligibleDrivers(
            @RequestParam(required = false) String serviceArea) {

        // Manual 400 guard for query param (not a DTO, so @NotBlank cannot be used here)
        if (serviceArea == null || serviceArea.isBlank()) {
            throw new IllegalArgumentException("serviceArea query parameter must not be blank");
        }

        return ResponseEntity.ok(driverService.getEligibleDrivers(serviceArea));
    }

    // -----------------------------------------------------------------------
    // GET /api/drivers/{driverId}
    // -----------------------------------------------------------------------

    @Operation(
            summary = "Get a driver profile by driverId",
            description = "Returns the full driver profile. Accessible by any authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver profile found",
                    content = @Content(schema = @Schema(implementation = DriverResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Driver not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{driverId}")
    public ResponseEntity<DriverResponse> getDriver(@PathVariable String driverId) {
        return ResponseEntity.ok(driverService.getDriver(driverId));
    }

    // -----------------------------------------------------------------------
    // PUT /api/drivers/{driverId}
    // -----------------------------------------------------------------------

    @Operation(
            summary = "Update driver service area",
            description = "Replaces the serviceArea field. Requires DRIVER role and ownership of the profile.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver profile updated",
                    content = @Content(schema = @Schema(implementation = DriverResponse.class))),
            @ApiResponse(responseCode = "400", description = "serviceArea is blank",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not the profile owner or wrong role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Driver not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{driverId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverResponse> updateServiceArea(
            @PathVariable String driverId,
            @Valid @RequestBody UpdateServiceAreaRequest request,
            Authentication auth) {

        String accountId = (String) auth.getPrincipal();
        return ResponseEntity.ok(driverService.updateServiceArea(driverId, accountId, request));
    }

    // -----------------------------------------------------------------------
    // PATCH /api/drivers/{driverId}/availability
    // -----------------------------------------------------------------------

    @Operation(
            summary = "Update driver availability",
            description = "Sets availability to AVAILABLE or UNAVAILABLE. "
                        + "Requires DRIVER role and ownership.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability updated",
                    content = @Content(schema = @Schema(implementation = AvailabilityResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid availability value (not AVAILABLE or UNAVAILABLE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not the profile owner or wrong role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Driver not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{driverId}/availability")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<AvailabilityResponse> updateAvailability(
            @PathVariable String driverId,
            @Valid @RequestBody UpdateAvailabilityRequest request,
            Authentication auth) {

        String accountId = (String) auth.getPrincipal();
        return ResponseEntity.ok(driverService.updateAvailability(driverId, accountId, request));
    }

    // -----------------------------------------------------------------------
    // PATCH /api/drivers/{driverId}/location
    // -----------------------------------------------------------------------

    @Operation(
            summary = "Update driver location",
            description = "Updates currentLatitude and currentLongitude. "
                        + "Requires DRIVER role and ownership.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location updated",
                    content = @Content(schema = @Schema(implementation = LocationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Coordinates out of valid range",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not the profile owner or wrong role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Driver not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{driverId}/location")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<LocationResponse> updateLocation(
            @PathVariable String driverId,
            @Valid @RequestBody UpdateLocationRequest request,
            Authentication auth) {

        String accountId = (String) auth.getPrincipal();
        return ResponseEntity.ok(driverService.updateLocation(driverId, accountId, request));
    }
}
