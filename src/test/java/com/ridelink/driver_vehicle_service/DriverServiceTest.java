package com.ridelink.driver_vehicle_service;

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
import com.ridelink.driver_vehicle_service.service.DriverService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DriverService}.
 *
 * All repository calls are mocked with Mockito.
 * DTO validation (400 cases) is exercised directly via Jakarta {@link Validator}
 * so no Spring application context is needed.
 */
@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private DriverService driverService;

    /** Jakarta Validator for DTO-level constraint checks (400 cases). */
    private Validator validator;

    @BeforeEach
    void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Returns a fully-populated Driver document saved in Mongo. */
    private Driver savedDriver(String accountId, String driverId, Availability availability) {
        return Driver.builder()
                .id("mongo-internal-id")
                .driverId(driverId)
                .accountId(accountId)
                .serviceArea("Colombo")
                .currentLatitude(6.9271)
                .currentLongitude(79.8612)
                .availability(availability)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST /api/drivers
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createDriver")
    class CreateDriverTests {

        @Test
        @DisplayName("success — returns full DriverResponse with UNAVAILABLE availability")
        void createDriver_success() {
            // Arrange
            String accountId = "acc-001";
            CreateDriverRequest req = new CreateDriverRequest();
            req.setServiceArea("Colombo");
            req.setCurrentLatitude(6.9271);
            req.setCurrentLongitude(79.8612);

            when(driverRepository.existsByAccountId(accountId)).thenReturn(false);
            Driver saved = savedDriver(accountId, "drv-uuid-001", Availability.UNAVAILABLE);
            when(driverRepository.save(any(Driver.class))).thenReturn(saved);

            // Act
            DriverResponse response = driverService.createDriver(accountId, req);

            // Assert
            assertThat(response.getAccountId()).isEqualTo(accountId);
            assertThat(response.getAvailability()).isEqualTo(Availability.UNAVAILABLE);
            assertThat(response.getServiceArea()).isEqualTo("Colombo");
            assertThat(response.getDriverId()).isEqualTo("drv-uuid-001");
            // internal Mongo id must NOT appear in response
            verify(driverRepository).save(any(Driver.class));
        }

        @Test
        @DisplayName("409 — duplicate accountId throws DuplicateDriverProfileException")
        void createDriver_duplicateAccountId_throwsConflict() {
            String accountId = "acc-duplicate";
            when(driverRepository.existsByAccountId(accountId)).thenReturn(true);

            CreateDriverRequest req = new CreateDriverRequest();
            req.setServiceArea("Galle");
            req.setCurrentLatitude(6.0);;
            req.setCurrentLongitude(80.2);

            assertThatThrownBy(() -> driverService.createDriver(accountId, req))
                    .isInstanceOf(DuplicateDriverProfileException.class)
                    .hasMessageContaining(accountId);

            verify(driverRepository, never()).save(any());
        }

        @Test
        @DisplayName("400 — latitude out of range fails bean validation")
        void createDriver_invalidLatitude_failsValidation() {
            CreateDriverRequest req = new CreateDriverRequest();
            req.setServiceArea("Kandy");
            req.setCurrentLatitude(200.0);   // out of [-90, 90]
            req.setCurrentLongitude(80.6);

            Set<ConstraintViolation<CreateDriverRequest>> violations = validator.validate(req);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("currentLatitude"));
        }

        @Test
        @DisplayName("400 — longitude out of range fails bean validation")
        void createDriver_invalidLongitude_failsValidation() {
            CreateDriverRequest req = new CreateDriverRequest();
            req.setServiceArea("Kandy");
            req.setCurrentLatitude(7.2906);
            req.setCurrentLongitude(-200.0);  // out of [-180, 180]

            Set<ConstraintViolation<CreateDriverRequest>> violations = validator.validate(req);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("currentLongitude"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/drivers/{driverId}
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getDriver")
    class GetDriverTests {

        @Test
        @DisplayName("success — returns full DriverResponse when driver exists")
        void getDriver_success() {
            String driverId = "drv-uuid-001";
            Driver existing = savedDriver("acc-001", driverId, Availability.AVAILABLE);

            when(driverRepository.findByDriverId(driverId)).thenReturn(Optional.of(existing));

            DriverResponse response = driverService.getDriver(driverId);

            assertThat(response.getDriverId()).isEqualTo(driverId);
            assertThat(response.getAccountId()).isEqualTo("acc-001");
            assertThat(response.getServiceArea()).isEqualTo("Colombo");
            assertThat(response.getAvailability()).isEqualTo(Availability.AVAILABLE);
            assertThat(response.getCurrentLatitude()).isEqualTo(6.9271);
            assertThat(response.getCurrentLongitude()).isEqualTo(79.8612);
            verify(driverRepository).findByDriverId(driverId);
        }

        @Test
        @DisplayName("404 — throws DriverNotFoundException when driver does not exist")
        void getDriver_notFound_throwsDriverNotFoundException() {
            String driverId = "drv-non-existent";
            when(driverRepository.findByDriverId(driverId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> driverService.getDriver(driverId))
                    .isInstanceOf(DriverNotFoundException.class)
                    .hasMessageContaining(driverId);

            verify(driverRepository).findByDriverId(driverId);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUT /api/drivers/{driverId}
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateServiceArea")
    class UpdateServiceAreaTests {

        @Test
        @DisplayName("success — updates serviceArea, refreshes updatedAt, and returns DriverResponse")
        void updateServiceArea_success() {
            String accountId = "acc-owner";
            String driverId  = "drv-uuid-001";

            Driver existing = savedDriver(accountId, driverId, Availability.UNAVAILABLE);
            Driver updated  = savedDriver(accountId, driverId, Availability.UNAVAILABLE);
            updated.setServiceArea("Kandy");
            updated.setUpdatedAt(Instant.now());

            when(driverRepository.findByDriverId(driverId)).thenReturn(Optional.of(existing));
            when(driverRepository.save(any(Driver.class))).thenReturn(updated);

            UpdateServiceAreaRequest req = new UpdateServiceAreaRequest();
            req.setServiceArea("Kandy");

            DriverResponse response = driverService.updateServiceArea(driverId, accountId, req);

            assertThat(response.getDriverId()).isEqualTo(driverId);
            assertThat(response.getServiceArea()).isEqualTo("Kandy");
            assertThat(response.getUpdatedAt()).isNotNull();
            verify(driverRepository).save(any(Driver.class));
        }

        @Test
        @DisplayName("404 — throws DriverNotFoundException when driver does not exist")
        void updateServiceArea_notFound_throwsDriverNotFoundException() {
            String driverId = "drv-non-existent";
            when(driverRepository.findByDriverId(driverId)).thenReturn(Optional.empty());

            UpdateServiceAreaRequest req = new UpdateServiceAreaRequest();
            req.setServiceArea("Kandy");

            assertThatThrownBy(() -> driverService.updateServiceArea(driverId, "acc-owner", req))
                    .isInstanceOf(DriverNotFoundException.class)
                    .hasMessageContaining(driverId);

            verify(driverRepository, never()).save(any());
        }

        @Test
        @DisplayName("403 — wrong owner throws ForbiddenOperationException")
        void updateServiceArea_wrongOwner_throwsForbidden() {
            String realOwner = "acc-owner";
            String intruder  = "acc-intruder";
            String driverId  = "drv-uuid-001";

            Driver existing = savedDriver(realOwner, driverId, Availability.UNAVAILABLE);
            when(driverRepository.findByDriverId(driverId)).thenReturn(Optional.of(existing));

            UpdateServiceAreaRequest req = new UpdateServiceAreaRequest();
            req.setServiceArea("Gampaha");

            assertThatThrownBy(() -> driverService.updateServiceArea(driverId, intruder, req))
                    .isInstanceOf(ForbiddenOperationException.class);

            verify(driverRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PATCH /api/drivers/{driverId}/availability
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateAvailability")
    class UpdateAvailabilityTests {

        @Test
        @DisplayName("success — returns AvailabilityResponse with updated value and updatedAt")
        void updateAvailability_success() {
            String accountId = "acc-001";
            String driverId  = "drv-uuid-001";

            Driver existing = savedDriver(accountId, driverId, Availability.UNAVAILABLE);
            Driver updated  = savedDriver(accountId, driverId, Availability.AVAILABLE);
            updated.setUpdatedAt(Instant.now());

            when(driverRepository.findByDriverId(driverId)).thenReturn(Optional.of(existing));
            when(driverRepository.save(any(Driver.class))).thenReturn(updated);

            UpdateAvailabilityRequest req = new UpdateAvailabilityRequest();
            req.setAvailability(Availability.AVAILABLE);

            AvailabilityResponse response = driverService.updateAvailability(driverId, accountId, req);

            assertThat(response.getDriverId()).isEqualTo(driverId);
            assertThat(response.getAvailability()).isEqualTo(Availability.AVAILABLE);
            assertThat(response.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("403 — wrong owner throws ForbiddenOperationException")
        void updateAvailability_wrongOwner_throwsForbidden() {
            String realOwner = "acc-owner";
            String intruder  = "acc-intruder";
            String driverId  = "drv-uuid-001";

            Driver existing = savedDriver(realOwner, driverId, Availability.UNAVAILABLE);
            when(driverRepository.findByDriverId(driverId)).thenReturn(Optional.of(existing));

            UpdateAvailabilityRequest req = new UpdateAvailabilityRequest();
            req.setAvailability(Availability.AVAILABLE);

            assertThatThrownBy(() -> driverService.updateAvailability(driverId, intruder, req))
                    .isInstanceOf(ForbiddenOperationException.class);

            verify(driverRepository, never()).save(any());
        }

        @Test
        @DisplayName("400 — null availability fails bean validation (simulates invalid enum string)")
        void updateAvailability_nullAvailability_failsValidation() {
            // In production, an invalid enum string from the client causes Jackson to
            // throw HttpMessageNotReadableException before the DTO even reaches the
            // service. Here we verify the @NotNull guard on the DTO itself.
            UpdateAvailabilityRequest req = new UpdateAvailabilityRequest();
            req.setAvailability(null);

            Set<ConstraintViolation<UpdateAvailabilityRequest>> violations = validator.validate(req);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("availability"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PATCH /api/drivers/{driverId}/location
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateLocation")
    class UpdateLocationTests {

        @Test
        @DisplayName("success — returns LocationResponse with new coordinates and updatedAt")
        void updateLocation_success() {
            String accountId = "acc-001";
            String driverId  = "drv-uuid-001";

            Driver existing = savedDriver(accountId, driverId, Availability.AVAILABLE);
            Driver updated  = savedDriver(accountId, driverId, Availability.AVAILABLE);
            updated.setCurrentLatitude(7.0);
            updated.setCurrentLongitude(80.0);
            updated.setUpdatedAt(Instant.now());

            when(driverRepository.findByDriverId(driverId)).thenReturn(Optional.of(existing));
            when(driverRepository.save(any(Driver.class))).thenReturn(updated);

            UpdateLocationRequest req = new UpdateLocationRequest();
            req.setCurrentLatitude(7.0);
            req.setCurrentLongitude(80.0);

            LocationResponse response = driverService.updateLocation(driverId, accountId, req);

            assertThat(response.getDriverId()).isEqualTo(driverId);
            assertThat(response.getCurrentLatitude()).isEqualTo(7.0);
            assertThat(response.getCurrentLongitude()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("400 — latitude 91.0 (out of [-90,90]) fails bean validation")
        void updateLocation_invalidLatitude_failsValidation() {
            UpdateLocationRequest req = new UpdateLocationRequest();
            req.setCurrentLatitude(91.0);    // > 90 → invalid
            req.setCurrentLongitude(80.0);

            Set<ConstraintViolation<UpdateLocationRequest>> violations = validator.validate(req);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("currentLatitude"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/drivers/eligible?serviceArea=X
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getEligibleDrivers")
    class GetEligibleDriversTests {

        @Test
        @DisplayName("returns only AVAILABLE drivers matching the serviceArea")
        void getEligibleDrivers_returnsMatchingAvailableDrivers() {
            String serviceArea = "Colombo";

            Driver d1 = savedDriver("acc-001", "drv-001", Availability.AVAILABLE);
            Driver d2 = savedDriver("acc-002", "drv-002", Availability.AVAILABLE);

            when(driverRepository.findByAvailabilityAndServiceArea(Availability.AVAILABLE, serviceArea))
                    .thenReturn(List.of(d1, d2));

            List<EligibleDriverResponse> results = driverService.getEligibleDrivers(serviceArea);

            assertThat(results).hasSize(2);
            assertThat(results).allMatch(r -> r.getAvailability() == Availability.AVAILABLE);
            assertThat(results).allMatch(r -> r.getServiceArea().equals("Colombo"));
            // accountId must be present (not userId)
            assertThat(results.get(0).getAccountId()).isEqualTo("acc-001");
        }

        @Test
        @DisplayName("returns empty list (not null, not 404) when no eligible drivers exist")
        void getEligibleDrivers_emptyList_whenNoneFound() {
            when(driverRepository.findByAvailabilityAndServiceArea(Availability.AVAILABLE, "Jaffna"))
                    .thenReturn(List.of());

            List<EligibleDriverResponse> results = driverService.getEligibleDrivers("Jaffna");

            assertThat(results).isNotNull();
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("400 — blank serviceArea throws IllegalArgumentException before hitting repository")
        void getEligibleDrivers_blankServiceArea_throwsIllegalArgument() {
            assertThatThrownBy(() -> driverService.getEligibleDrivers("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("serviceArea");

            // Repository must never be called when input is invalid
            verify(driverRepository, never())
                    .findByAvailabilityAndServiceArea(any(), any());
        }
    }
}
