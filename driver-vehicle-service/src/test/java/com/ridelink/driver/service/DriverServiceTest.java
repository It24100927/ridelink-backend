package com.ridelink.driver.service;

import com.ridelink.driver.dto.request.CreateDriverProfileRequest;
import com.ridelink.driver.dto.request.UpdateAvailabilityRequest;
import com.ridelink.driver.dto.response.DriverProfileResponse;
import com.ridelink.driver.entity.DriverProfile;
import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.enums.DriverStatus;
import com.ridelink.driver.exception.BusinessValidationException;
import com.ridelink.driver.repository.DriverProfileRepository;
import com.ridelink.driver.repository.VehicleRepository; // Added import
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverProfileRepository driverProfileRepository;

    // Added this mock for MongoDB vehicle fetching
    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private DriverService driverService;

    @Test
    void createDriverProfile_Success() {
        // Arrange
        CreateDriverProfileRequest request = CreateDriverProfileRequest.builder()
                .accountId("acc123")
                .firstName("John")
                .lastName("Doe")
                .licenseNumber("LIC12345")
                .phoneNumber("1234567890")
                .build();

        when(driverProfileRepository.existsByAccountId("acc123")).thenReturn(false);
        when(driverProfileRepository.existsByLicenseNumber("LIC12345")).thenReturn(false);
        when(driverProfileRepository.save(any(DriverProfile.class))).thenAnswer(i -> {
            DriverProfile d = i.getArgument(0);
            d.setId("driver1");
            return d;
        });

        // Mock the new MongoDB vehicle fetch inside mapToResponse
        when(vehicleRepository.findByDriverId(anyString())).thenReturn(java.util.List.of());

        // Act
        DriverProfileResponse response = driverService.createDriverProfile(request, "acc123");

        // Assert
        assertNotNull(response);
        assertEquals("John", response.getFirstName());
        assertEquals(DriverStatus.PENDING_VERIFICATION, response.getStatus());
        verify(driverProfileRepository, times(1)).save(any(DriverProfile.class));
        verify(vehicleRepository, times(1)).findByDriverId("driver1"); // Verify vehicle fetch
    }

    @Test
    void updateAvailability_ThrowsException_WhenNoActiveVehicle() {
        // Arrange
        // Removed .vehicles(...) because MongoDB DriverProfile no longer holds vehicles
        DriverProfile profile = DriverProfile.builder()
                .id("driver1")
                .accountId("acc123")
                .status(DriverStatus.ACTIVE)
                .availabilityStatus(AvailabilityStatus.OFFLINE)
                .build();

        when(driverProfileRepository.findByAccountId("acc123")).thenReturn(Optional.of(profile));
        
        // Mock the repository check to return an empty list (simulating no active vehicles)
        when(vehicleRepository.findByDriverIdAndIsActiveTrue("driver1")).thenReturn(java.util.List.of());

        // Act & Assert
        BusinessValidationException exception = assertThrows(BusinessValidationException.class, () -> {
            driverService.updateAvailability("acc123", 
                new UpdateAvailabilityRequest(AvailabilityStatus.AVAILABLE));
        });

        assertEquals("Cannot set availability to AVAILABLE without an active vehicle", exception.getMessage());
        verify(vehicleRepository, times(1)).findByDriverIdAndIsActiveTrue("driver1");
    }
}