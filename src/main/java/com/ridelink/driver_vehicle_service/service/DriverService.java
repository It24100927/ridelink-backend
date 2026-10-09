package com.ridelink.driver_vehicle_service.service;

import com.ridelink.driver_vehicle_service.dto.request.CreateDriverRequest;
import com.ridelink.driver_vehicle_service.dto.request.UpdateAvailabilityRequest;
import com.ridelink.driver_vehicle_service.dto.request.UpdateLocationRequest;
import com.ridelink.driver_vehicle_service.dto.request.UpdateServiceAreaRequest;
import com.ridelink.driver_vehicle_service.dto.response.AvailabilityResponse;
import com.ridelink.driver_vehicle_service.dto.response.DriverResponse;
import com.ridelink.driver_vehicle_service.dto.response.EligibleDriverResponse;
import com.ridelink.driver_vehicle_service.dto.response.LocationResponse;
import com.ridelink.driver_vehicle_service.exception.DuplicateDriverProfileException;
import com.ridelink.driver_vehicle_service.exception.DriverNotFoundException;
import com.ridelink.driver_vehicle_service.exception.ForbiddenOperationException;
import com.ridelink.driver_vehicle_service.model.Driver;
import com.ridelink.driver_vehicle_service.model.Driver.Availability;
import com.ridelink.driver_vehicle_service.repository.DriverRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for all Driver-related operations.
 *
 * Role enforcement design decision:
 *   Role checks (DRIVER-only) are applied at the controller layer via
 *   {@code @PreAuthorize("hasRole('DRIVER')")}.
 *   Ownership checks (the authenticated accountId must match the Driver's
 *   accountId) are done here in the service layer, throwing
 *   {@link ForbiddenOperationException} on mismatch.
 *   This keeps security concerns separated cleanly.
 */
@Service
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    // -----------------------------------------------------------------------
    // POST /api/drivers
    // -----------------------------------------------------------------------

    /**
     * Creates a new Driver profile.
     *
     * @param accountId extracted from JWT subject — never from request body
     * @param request   validated request body
     * @return full DriverResponse (HTTP 201)
     * @throws DuplicateDriverProfileException if accountId already has a profile (409)
     */
    public DriverResponse createDriver(String accountId, CreateDriverRequest request) {
        if (driverRepository.existsByAccountId(accountId)) {
            throw new DuplicateDriverProfileException(accountId);
        }

        Instant now = Instant.now();
        Driver driver = Driver.builder()
                .driverId(UUID.randomUUID().toString())
                .accountId(accountId)
                .serviceArea(request.getServiceArea())
                .currentLatitude(request.getCurrentLatitude())
                .currentLongitude(request.getCurrentLongitude())
                .availability(Availability.UNAVAILABLE)   // default per contract
                .createdAt(now)
                .updatedAt(now)
                .build();

        return DriverResponse.from(driverRepository.save(driver));
    }

    // -----------------------------------------------------------------------
    // GET /api/drivers/{driverId}
    // -----------------------------------------------------------------------

    /**
     * Loads a driver by public driverId.
     *
     * @throws DriverNotFoundException if not found (404)
     */
    public DriverResponse getDriver(String driverId) {
        Driver driver = driverRepository.findByDriverId(driverId)
                .orElseThrow(() -> new DriverNotFoundException(driverId));
        return DriverResponse.from(driver);
    }

    // -----------------------------------------------------------------------
    // PUT /api/drivers/{driverId}
    // -----------------------------------------------------------------------

    /**
     * Replaces the serviceArea of an existing driver.
     *
     * @param driverId  path variable — public UUID
     * @param accountId from JWT subject — used for ownership check
     * @param request   validated body (serviceArea only)
     * @throws DriverNotFoundException    if driverId not found (404)
     * @throws ForbiddenOperationException if JWT accountId ≠ driver's accountId (403)
     */
    public DriverResponse updateServiceArea(String driverId,
                                            String accountId,
                                            UpdateServiceAreaRequest request) {
        Driver driver = driverRepository.findByDriverId(driverId)
                .orElseThrow(() -> new DriverNotFoundException(driverId));

        assertOwner(driver.getAccountId(), accountId);

        driver.setServiceArea(request.getServiceArea());
        driver.setUpdatedAt(Instant.now());
        return DriverResponse.from(driverRepository.save(driver));
    }

    // -----------------------------------------------------------------------
    // PATCH /api/drivers/{driverId}/availability
    // -----------------------------------------------------------------------

    /**
     * Updates only the availability field.
     *
     * @throws DriverNotFoundException    if driverId not found (404)
     * @throws ForbiddenOperationException if not the owner (403)
     */
    public AvailabilityResponse updateAvailability(String driverId,
                                                   String accountId,
                                                   UpdateAvailabilityRequest request) {
        Driver driver = driverRepository.findByDriverId(driverId)
                .orElseThrow(() -> new DriverNotFoundException(driverId));

        assertOwner(driver.getAccountId(), accountId);

        driver.setAvailability(request.getAvailability());
        driver.setUpdatedAt(Instant.now());
        return AvailabilityResponse.from(driverRepository.save(driver));
    }

    // -----------------------------------------------------------------------
    // PATCH /api/drivers/{driverId}/location
    // -----------------------------------------------------------------------

    /**
     * Updates only the current coordinates.
     *
     * @throws DriverNotFoundException    if driverId not found (404)
     * @throws ForbiddenOperationException if not the owner (403)
     */
    public LocationResponse updateLocation(String driverId,
                                           String accountId,
                                           UpdateLocationRequest request) {
        Driver driver = driverRepository.findByDriverId(driverId)
                .orElseThrow(() -> new DriverNotFoundException(driverId));

        assertOwner(driver.getAccountId(), accountId);

        driver.setCurrentLatitude(request.getCurrentLatitude());
        driver.setCurrentLongitude(request.getCurrentLongitude());
        driver.setUpdatedAt(Instant.now());
        return LocationResponse.from(driverRepository.save(driver));
    }

    // -----------------------------------------------------------------------
    // GET /api/drivers/eligible?serviceArea=X
    // -----------------------------------------------------------------------

    /**
     * Returns all AVAILABLE drivers in the given service area.
     * Always returns a list (empty list when none found — never null, never 404).
     *
     * @param serviceArea the service area to filter by — must not be null or blank
     * @throws IllegalArgumentException if serviceArea is null or blank (400)
     */
    public List<EligibleDriverResponse> getEligibleDrivers(String serviceArea) {
        if (serviceArea == null || serviceArea.isBlank()) {
            throw new IllegalArgumentException(
                    "serviceArea must not be blank");
        }
        return driverRepository
                .findByAvailabilityAndServiceArea(Availability.AVAILABLE, serviceArea)
                .stream()
                .map(EligibleDriverResponse::from)
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Throws {@link ForbiddenOperationException} when the resource's owning
     * accountId does not match the authenticated accountId from the JWT.
     */
    private void assertOwner(String resourceAccountId, String jwtAccountId) {
        if (!resourceAccountId.equals(jwtAccountId)) {
            throw new ForbiddenOperationException(
                    "You are not authorised to modify this resource");
        }
    }
}
