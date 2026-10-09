package com.ridelink.driver.service;

import com.ridelink.driver.dto.response.EligibleDriverResponse;
import com.ridelink.driver.entity.DriverProfile;
import com.ridelink.driver.entity.Vehicle;
import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.enums.DriverStatus;
import com.ridelink.driver.enums.VehicleType;
import com.ridelink.driver.repository.DriverProfileRepository;
import com.ridelink.driver.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EligibleDriverServiceTest {

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private EligibleDriverService eligibleDriverService;

    private DriverProfile availableDriver;
    private Vehicle availableVehicle;

    @BeforeEach
    void setUp() {
        // Vehicle stored in its own collection (MongoDB)
        availableVehicle = Vehicle.builder()
                .id("vehicle-1")
                .driverId("driver-1")
                .registrationNumber("ABC-1234")
                .make("Toyota")
                .model("Camry")
                .vehicleType(VehicleType.ECONOMY)
                .passengerCapacity(4)
                .isActive(true)
                .build();

        // Driver WITHOUT vehicles list (MongoDB migration)
        availableDriver = DriverProfile.builder()
                .id("driver-1")
                .accountId("account-1")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .status(DriverStatus.ACTIVE)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .currentLatitude(6.9271)
                .currentLongitude(79.8612)
                .averageRating(4.5)
                .totalRides(100)
                .build();
    }

    @Test
    @DisplayName("Should find eligible drivers within radius")
    void shouldFindEligibleDrivers() {
        when(driverProfileRepository.findAllAvailableDrivers())
                .thenReturn(List.of(availableDriver));
        when(vehicleRepository.findByDriverIdAndIsActiveTrue("driver-1"))
                .thenReturn(List.of(availableVehicle));

        List<EligibleDriverResponse> result = eligibleDriverService.findEligibleDrivers(
                6.9271, 79.8612, null, null, 10.0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDriverId()).isEqualTo("driver-1");
        assertThat(result.get(0).getDistanceKm()).isNotNull();
    }

    @Test
    @DisplayName("Should filter by vehicle type")
    void shouldFilterByVehicleType() {
        when(driverProfileRepository.findAllAvailableDrivers())
                .thenReturn(List.of(availableDriver));
        when(vehicleRepository.findByDriverIdAndIsActiveTrue("driver-1"))
                .thenReturn(List.of(availableVehicle));

        // Look for PREMIUM, but driver has ECONOMY
        List<EligibleDriverResponse> result = eligibleDriverService.findEligibleDrivers(
                null, null, VehicleType.PREMIUM, null, 10.0);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should filter by passenger capacity")
    void shouldFilterByCapacity() {
        when(driverProfileRepository.findAllAvailableDrivers())
                .thenReturn(List.of(availableDriver));
        when(vehicleRepository.findByDriverIdAndIsActiveTrue("driver-1"))
                .thenReturn(List.of(availableVehicle));

        // Look for capacity 6, but driver has capacity 4
        List<EligibleDriverResponse> result = eligibleDriverService.findEligibleDrivers(
                null, null, null, 6, 10.0);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return drivers sorted by distance")
    void shouldSortByDistance() {
        // Move driver-1 to a point ~3 km away so it isn't sitting on the pickup point
        availableDriver.setCurrentLatitude(6.9500);
        availableDriver.setCurrentLongitude(79.8900);

        // Setup a genuinely closer driver (~0.05 km from pickup)
        DriverProfile closerDriver = DriverProfile.builder()
                .id("driver-close")
                .accountId("account-close")
                .firstName("Close")
                .lastName("Driver")
                .status(DriverStatus.ACTIVE)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .currentLatitude(6.9272)
                .currentLongitude(79.8615)
                .build();

        Vehicle closerVehicle = Vehicle.builder()
                .id("v-close")
                .driverId("driver-close")
                .vehicleType(VehicleType.ECONOMY)
                .passengerCapacity(4)
                .isActive(true)
                .build();

        when(driverProfileRepository.findAllAvailableDrivers())
                .thenReturn(List.of(availableDriver, closerDriver));
        when(vehicleRepository.findByDriverIdAndIsActiveTrue("driver-1"))
                .thenReturn(List.of(availableVehicle));
        when(vehicleRepository.findByDriverIdAndIsActiveTrue("driver-close"))
                .thenReturn(List.of(closerVehicle));

        List<EligibleDriverResponse> result = eligibleDriverService.findEligibleDrivers(
                6.9271, 79.8612, null, null, 10.0);

        assertThat(result).hasSize(2);
        // The closer driver MUST appear first
        assertThat(result.get(0).getDriverId()).isEqualTo("driver-close");
        assertThat(result.get(0).getDistanceKm())
                .isLessThan(result.get(1).getDistanceKm());
    }
}