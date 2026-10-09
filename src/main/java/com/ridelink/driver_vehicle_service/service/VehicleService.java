package com.ridelink.driver_vehicle_service.service;

import com.ridelink.driver_vehicle_service.dto.request.CreateVehicleRequest;
import com.ridelink.driver_vehicle_service.dto.request.UpdateVehicleRequest;
import com.ridelink.driver_vehicle_service.dto.response.VehicleResponse;
import com.ridelink.driver_vehicle_service.exception.DriverNotFoundException;
import com.ridelink.driver_vehicle_service.exception.DuplicateVehicleNumberException;
import com.ridelink.driver_vehicle_service.exception.ForbiddenOperationException;
import com.ridelink.driver_vehicle_service.exception.VehicleNotFoundException;
import com.ridelink.driver_vehicle_service.model.Driver;
import com.ridelink.driver_vehicle_service.model.Vehicle;
import com.ridelink.driver_vehicle_service.repository.DriverRepository;
import com.ridelink.driver_vehicle_service.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Business logic for all Vehicle-related operations.
 *
 * driverId is NEVER accepted from the client request body.
 * It is always derived server-side by looking up the Driver whose
 * accountId matches the JWT subject claim.
 */
@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository  driverRepository;

    public VehicleService(VehicleRepository vehicleRepository,
                          DriverRepository driverRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository  = driverRepository;
    }

    // -----------------------------------------------------------------------
    // POST /api/vehicles
    // -----------------------------------------------------------------------

    /**
     * Registers a new vehicle for the authenticated driver.
     *
     * <p>Business rules:
     * <ol>
     *   <li>Look up Driver by {@code accountId} from JWT. If no Driver profile
     *       exists yet the driver has no operational profile and cannot register
     *       a vehicle — return 404. This enforces the invariant that a vehicle
     *       must always be owned by an existing Driver profile.</li>
     *   <li>Reject with 409 if the vehicleNumber is already registered.</li>
     *   <li>Generate vehicleId (UUID), set createdAt = updatedAt = now, save.</li>
     * </ol>
     *
     * @param accountId from JWT subject — used to resolve the owning Driver
     * @param request   validated request body
     * @return VehicleResponse (HTTP 201)
     * @throws DriverNotFoundException        if no Driver profile exists for this account (404)
     * @throws DuplicateVehicleNumberException if vehicleNumber already taken (409)
     */
    public VehicleResponse createVehicle(String accountId, CreateVehicleRequest request) {
        // Rule 1: driver must have an existing operational profile
        Driver driver = driverRepository.findByAccountId(accountId)
                .orElseThrow(() -> new DriverNotFoundException(
                        "No driver profile found for account: " + accountId));

        // Rule 2: vehicleNumber uniqueness
        if (vehicleRepository.existsByVehicleNumber(request.getVehicleNumber())) {
            throw new DuplicateVehicleNumberException(request.getVehicleNumber());
        }

        Instant now = Instant.now();
        Vehicle vehicle = Vehicle.builder()
                .vehicleId(UUID.randomUUID().toString())
                .driverId(driver.getDriverId())          // server-derived, not from client
                .vehicleNumber(request.getVehicleNumber())
                .vehicleType(request.getVehicleType())
                .model(request.getModel())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    // -----------------------------------------------------------------------
    // PUT /api/vehicles/{vehicleId}
    // -----------------------------------------------------------------------

    /**
     * Fully updates a vehicle's mutable fields (vehicleNumber, vehicleType, model).
     *
     * <p>Business rules:
     * <ol>
     *   <li>Load vehicle by vehicleId — 404 if not found.</li>
     *   <li>Resolve the owning Driver by the vehicle's driverId, then compare
     *       that driver's accountId against the JWT accountId — 403 if mismatch.</li>
     *   <li>Update all three fields + updatedAt, save.</li>
     * </ol>
     *
     * @param vehicleId  path variable — public UUID
     * @param accountId  from JWT subject — used for ownership check
     * @param request    validated request body
     * @throws VehicleNotFoundException    if vehicleId not found (404)
     * @throws ForbiddenOperationException if authenticated user does not own vehicle (403)
     */
    public VehicleResponse updateVehicle(String vehicleId,
                                         String accountId,
                                         UpdateVehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findByVehicleId(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        // Ownership check: resolve the driver who owns this vehicle
        Driver owner = driverRepository.findByDriverId(vehicle.getDriverId())
                .orElseThrow(() -> new DriverNotFoundException(vehicle.getDriverId()));

        if (!owner.getAccountId().equals(accountId)) {
            throw new ForbiddenOperationException(
                    "You are not authorised to modify this vehicle");
        }

        vehicle.setVehicleNumber(request.getVehicleNumber());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setModel(request.getModel());
        vehicle.setUpdatedAt(Instant.now());

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }
}
