package com.ridelink.driver.controller;

import com.ridelink.driver.dto.request.*;
import com.ridelink.driver.dto.response.ApiResponse;
import com.ridelink.driver.dto.response.DriverProfileResponse;
import com.ridelink.driver.dto.response.EligibleDriverResponse;
import com.ridelink.driver.dto.response.VehicleResponse;
import com.ridelink.driver.enums.DriverStatus;
import com.ridelink.driver.enums.VehicleType;
import com.ridelink.driver.service.DriverService;
import com.ridelink.driver.service.EligibleDriverService;
import com.ridelink.driver.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Tag(name = "Driver Management", description = "Driver profile and operational management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DriverController {

    private final DriverService driverService;
    private final VehicleService vehicleService;
    private final EligibleDriverService eligibleDriverService;

    // ==================== Driver Profile Endpoints ====================

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Create driver profile",
            description = "Creates a new driver operational profile for the authenticated driver account")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Profile created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid input or profile already exists"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<DriverProfileResponse>> createDriverProfile(
            @Valid @RequestBody CreateDriverProfileRequest request,
            @AuthenticationPrincipal String accountId) {

        DriverProfileResponse response = driverService.createDriverProfile(request, accountId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Driver profile created successfully"));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get my driver profile",
            description = "Retrieves the authenticated driver's operational profile")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> getMyProfile(
            @AuthenticationPrincipal String accountId) {

        DriverProfileResponse response = driverService.getDriverProfileByAccountId(accountId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Update my driver profile",
            description = "Updates the authenticated driver's operational profile")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal String accountId,
            @Valid @RequestBody UpdateDriverProfileRequest request) {

        DriverProfileResponse response = driverService.updateDriverProfile(accountId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Profile updated successfully"));
    }

    @PatchMapping("/me/availability")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Update availability status",
            description = "Updates the driver's availability status (AVAILABLE, UNAVAILABLE, ON_RIDE, OFFLINE)")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> updateAvailability(
            @AuthenticationPrincipal String accountId,
            @Valid @RequestBody UpdateAvailabilityRequest request) {

        DriverProfileResponse response = driverService.updateAvailability(accountId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Availability updated successfully"));
    }

    @PatchMapping("/me/location")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Update current location",
            description = "Updates the driver's simulated current location")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> updateLocation(
            @AuthenticationPrincipal String accountId,
            @Valid @RequestBody UpdateLocationRequest request) {

        DriverProfileResponse response = driverService.updateLocation(accountId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Location updated successfully"));
    }

    @GetMapping("/{driverId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    @Operation(summary = "Get driver profile by ID",
            description = "Retrieves a driver profile by driver ID (Admin or self only)")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> getDriverById(
            @PathVariable String driverId) {

        DriverProfileResponse response = driverService.getDriverProfileById(driverId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== Admin Endpoints ====================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all drivers",
            description = "Retrieves all driver profiles (Admin only)")
    public ResponseEntity<ApiResponse<List<DriverProfileResponse>>> getAllDrivers() {
        List<DriverProfileResponse> drivers = driverService.getAllDrivers();
        return ResponseEntity.ok(ApiResponse.success(drivers));
    }

    @PatchMapping("/{driverId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update driver status",
            description = "Updates a driver's account status (Admin only)")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> updateDriverStatus(
            @PathVariable String driverId,
            @RequestParam DriverStatus status) {

        DriverProfileResponse response = driverService.updateDriverStatus(driverId, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Driver status updated"));
    }

    @DeleteMapping("/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete driver profile",
            description = "Deletes a driver profile (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteDriverProfile(@PathVariable String driverId) {
        driverService.deleteDriverProfile(driverId);
        return ResponseEntity.ok(ApiResponse.success(null, "Driver profile deleted"));
    }

    // ==================== Vehicle Endpoints ====================

    @PostMapping("/me/vehicles")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Add a vehicle",
            description = "Adds a new vehicle to the authenticated driver's profile")
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            @AuthenticationPrincipal String accountId,
            @Valid @RequestBody CreateVehicleRequest request) {

        VehicleResponse response = vehicleService.createVehicle(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Vehicle added successfully"));
    }

    @GetMapping("/me/vehicles")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get my vehicles",
            description = "Retrieves all vehicles for the authenticated driver")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getMyVehicles(
            @AuthenticationPrincipal String accountId) {

        List<VehicleResponse> vehicles = vehicleService.getVehiclesByAccountId(accountId);
        return ResponseEntity.ok(ApiResponse.success(vehicles));
    }

    @GetMapping("/me/vehicles/{vehicleId}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get vehicle by ID",
            description = "Retrieves a specific vehicle by ID")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicleById(
            @AuthenticationPrincipal String accountId,
            @PathVariable String vehicleId) {

        VehicleResponse response = vehicleService.getVehicleById(accountId, vehicleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me/vehicles/{vehicleId}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Update vehicle",
            description = "Updates a vehicle's details")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            @AuthenticationPrincipal String accountId,
            @PathVariable String vehicleId,
            @Valid @RequestBody UpdateVehicleRequest request) {

        VehicleResponse response = vehicleService.updateVehicle(accountId, vehicleId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Vehicle updated successfully"));
    }

    @DeleteMapping("/me/vehicles/{vehicleId}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Delete vehicle",
            description = "Removes a vehicle from the driver's profile")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(
            @AuthenticationPrincipal String accountId,
            @PathVariable String vehicleId) {

        vehicleService.deleteVehicle(accountId, vehicleId);
        return ResponseEntity.ok(ApiResponse.success(null, "Vehicle deleted successfully"));
    }

    // ==================== Internal/Service-to-Service Endpoints ====================

    @GetMapping("/eligible")
    @Operation(summary = "Find eligible drivers",
            description = "Internal endpoint for Ride Management Service to find eligible available drivers")
    public ResponseEntity<ApiResponse<List<EligibleDriverResponse>>> findEligibleDrivers(
            @Parameter(description = "Pickup latitude")
            @RequestParam(required = false) Double pickupLatitude,

            @Parameter(description = "Pickup longitude")
            @RequestParam(required = false) Double pickupLongitude,

            @Parameter(description = "Required vehicle type")
            @RequestParam(required = false) VehicleType vehicleType,

            @Parameter(description = "Required passenger capacity")
            @RequestParam(required = false) Integer requiredCapacity,

            @Parameter(description = "Search radius in kilometers")
            @RequestParam(required = false, defaultValue = "10.0") Double searchRadiusKm) {

        List<EligibleDriverResponse> drivers = eligibleDriverService.findEligibleDrivers(
                pickupLatitude, pickupLongitude, vehicleType, requiredCapacity, searchRadiusKm);

        return ResponseEntity.ok(ApiResponse.success(drivers,
                String.format("Found %d eligible drivers", drivers.size())));
    }
}