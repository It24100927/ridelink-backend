package com.ridelink.driver_vehicle_service;

import com.ridelink.driver_vehicle_service.dto.request.CreateVehicleRequest;
import com.ridelink.driver_vehicle_service.dto.request.UpdateVehicleRequest;
import com.ridelink.driver_vehicle_service.dto.response.VehicleResponse;
import com.ridelink.driver_vehicle_service.exception.DriverNotFoundException;
import com.ridelink.driver_vehicle_service.exception.DuplicateVehicleNumberException;
import com.ridelink.driver_vehicle_service.exception.ForbiddenOperationException;
import com.ridelink.driver_vehicle_service.exception.VehicleNotFoundException;
import com.ridelink.driver_vehicle_service.model.Driver;
import com.ridelink.driver_vehicle_service.model.Driver.Availability;
import com.ridelink.driver_vehicle_service.model.Vehicle;
import com.ridelink.driver_vehicle_service.repository.DriverRepository;
import com.ridelink.driver_vehicle_service.repository.VehicleRepository;
import com.ridelink.driver_vehicle_service.service.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link VehicleService}.
 *
 * All repository calls are mocked with Mockito.
 */
@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private VehicleService vehicleService;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Driver driver(String accountId, String driverId) {
        return Driver.builder()
                .id("mongo-id")
                .driverId(driverId)
                .accountId(accountId)
                .serviceArea("Colombo")
                .currentLatitude(6.9271)
                .currentLongitude(79.8612)
                .availability(Availability.AVAILABLE)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private Vehicle vehicle(String vehicleId, String driverId, String vehicleNumber) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return Vehicle.builder()
                .id("mongo-v-id")
                .vehicleId(vehicleId)
                .driverId(driverId)
                .vehicleNumber(vehicleNumber)
                .vehicleType("SEDAN")
                .model("Toyota Prius")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private CreateVehicleRequest createRequest(String vehicleNumber) {
        CreateVehicleRequest req = new CreateVehicleRequest();
        req.setVehicleNumber(vehicleNumber);
        req.setVehicleType("SEDAN");
        req.setModel("Toyota Prius");
        return req;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST /api/vehicles
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createVehicle")
    class CreateVehicleTests {

        @Test
        @DisplayName("success — driverId derived from accountId, vehicle saved and returned")
        void createVehicle_success() {
            String accountId    = "acc-001";
            String driverId     = "drv-uuid-001";
            String vehicleNumber = "CAB-1234";

            Driver drv = driver(accountId, driverId);
            Vehicle saved = vehicle("veh-uuid-001", driverId, vehicleNumber);

            when(driverRepository.findByAccountId(accountId)).thenReturn(Optional.of(drv));
            when(vehicleRepository.existsByVehicleNumber(vehicleNumber)).thenReturn(false);
            when(vehicleRepository.save(any(Vehicle.class))).thenReturn(saved);

            VehicleResponse response = vehicleService.createVehicle(accountId, createRequest(vehicleNumber));

            assertThat(response.getVehicleId()).isEqualTo("veh-uuid-001");
            // driverId must be derived from the driver profile, not from client
            assertThat(response.getDriverId()).isEqualTo(driverId);
            assertThat(response.getVehicleNumber()).isEqualTo(vehicleNumber);
            assertThat(response.getCreatedAt()).isNotNull();

            verify(vehicleRepository).save(any(Vehicle.class));
        }

        @Test
        @DisplayName("409 — duplicate vehicleNumber throws DuplicateVehicleNumberException")
        void createVehicle_duplicateVehicleNumber_throwsConflict() {
            String accountId    = "acc-001";
            String vehicleNumber = "DUP-9999";

            when(driverRepository.findByAccountId(accountId))
                    .thenReturn(Optional.of(driver(accountId, "drv-uuid-001")));
            when(vehicleRepository.existsByVehicleNumber(vehicleNumber)).thenReturn(true);

            assertThatThrownBy(() -> vehicleService.createVehicle(accountId, createRequest(vehicleNumber)))
                    .isInstanceOf(DuplicateVehicleNumberException.class)
                    .hasMessageContaining(vehicleNumber);

            verify(vehicleRepository, never()).save(any());
        }

        @Test
        @DisplayName("404 — no driver profile for this account throws DriverNotFoundException")
        void createVehicle_noDriverProfile_throwsNotFound() {
            String accountId = "acc-no-profile";
            when(driverRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vehicleService.createVehicle(accountId, createRequest("NEW-1111")))
                    .isInstanceOf(DriverNotFoundException.class);

            verify(vehicleRepository, never()).existsByVehicleNumber(any());
            verify(vehicleRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUT /api/vehicles/{vehicleId}
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateVehicle")
    class UpdateVehicleTests {

        @Test
        @DisplayName("success — all three fields updated, updatedAt refreshed")
        void updateVehicle_success() {
            String accountId  = "acc-owner";
            String driverId   = "drv-uuid-owner";
            String vehicleId  = "veh-uuid-001";

            Vehicle existing = vehicle(vehicleId, driverId, "OLD-1111");
            Driver  owner    = driver(accountId, driverId);

            Vehicle updated = vehicle(vehicleId, driverId, "NEW-2222");
            updated.setVehicleType("VAN");
            updated.setModel("Toyota HiAce");
            updated.setUpdatedAt(Instant.now());

            when(vehicleRepository.findByVehicleId(vehicleId)).thenReturn(Optional.of(existing));
            when(driverRepository.findByDriverId(driverId)).thenReturn(Optional.of(owner));
            when(vehicleRepository.save(any(Vehicle.class))).thenReturn(updated);

            UpdateVehicleRequest req = new UpdateVehicleRequest();
            req.setVehicleNumber("NEW-2222");
            req.setVehicleType("VAN");
            req.setModel("Toyota HiAce");

            VehicleResponse response = vehicleService.updateVehicle(vehicleId, accountId, req);

            assertThat(response.getVehicleNumber()).isEqualTo("NEW-2222");
            assertThat(response.getVehicleType()).isEqualTo("VAN");
            assertThat(response.getModel()).isEqualTo("Toyota HiAce");
            assertThat(response.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("403 — authenticated user does not own vehicle throws ForbiddenOperationException")
        void updateVehicle_wrongOwner_throwsForbidden() {
            String realOwnerAccountId = "acc-owner";
            String intruderAccountId  = "acc-intruder";
            String driverId           = "drv-uuid-owner";
            String vehicleId          = "veh-uuid-001";

            Vehicle existing = vehicle(vehicleId, driverId, "CAB-1234");
            // The owning driver's accountId is realOwnerAccountId
            Driver owner = driver(realOwnerAccountId, driverId);

            when(vehicleRepository.findByVehicleId(vehicleId)).thenReturn(Optional.of(existing));
            when(driverRepository.findByDriverId(driverId)).thenReturn(Optional.of(owner));

            UpdateVehicleRequest req = new UpdateVehicleRequest();
            req.setVehicleNumber("CAB-9999");
            req.setVehicleType("VAN");
            req.setModel("Ford Transit");

            // intruder's accountId ≠ owner's accountId → 403
            assertThatThrownBy(() -> vehicleService.updateVehicle(vehicleId, intruderAccountId, req))
                    .isInstanceOf(ForbiddenOperationException.class);

            verify(vehicleRepository, never()).save(any());
        }

        @Test
        @DisplayName("404 — vehicle not found throws VehicleNotFoundException")
        void updateVehicle_vehicleNotFound_throwsNotFound() {
            String vehicleId = "non-existent-veh";
            when(vehicleRepository.findByVehicleId(vehicleId)).thenReturn(Optional.empty());

            UpdateVehicleRequest req = new UpdateVehicleRequest();
            req.setVehicleNumber("ANY-0000");
            req.setVehicleType("SEDAN");
            req.setModel("Honda Civic");

            assertThatThrownBy(() -> vehicleService.updateVehicle(vehicleId, "any-account", req))
                    .isInstanceOf(VehicleNotFoundException.class)
                    .hasMessageContaining(vehicleId);

            verify(vehicleRepository, never()).save(any());
        }
    }
}
