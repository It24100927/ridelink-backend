package com.ridelink.driver.service;

import com.ridelink.driver.dto.request.CreateDriverProfileRequest;
import com.ridelink.driver.dto.request.UpdateAvailabilityRequest;
import com.ridelink.driver.dto.request.UpdateDriverProfileRequest;
import com.ridelink.driver.dto.request.UpdateLocationRequest;
import com.ridelink.driver.dto.response.DriverProfileResponse;
import com.ridelink.driver.dto.response.VehicleResponse;
import com.ridelink.driver.entity.DriverProfile;
import com.ridelink.driver.entity.Vehicle;
import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.enums.DriverStatus;
import com.ridelink.driver.exception.BusinessValidationException;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.exception.UnauthorizedException;
import com.ridelink.driver.repository.DriverProfileRepository;
import com.ridelink.driver.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public DriverProfileResponse createDriverProfile(
            CreateDriverProfileRequest request, String authenticatedAccountId) {

        log.info("Creating driver profile for account: {}", authenticatedAccountId);

        if (!request.getAccountId().equals(authenticatedAccountId)) {
            throw new UnauthorizedException("Cannot create profile for another account");
        }

        if (driverProfileRepository.existsByAccountId(request.getAccountId())) {
            throw new BusinessValidationException("Driver profile already exists for this account");
        }

        if (driverProfileRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new BusinessValidationException("License number already registered");
        }

        DriverProfile profile = DriverProfile.builder()
                .accountId(request.getAccountId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .licenseNumber(request.getLicenseNumber())
                .phoneNumber(request.getPhoneNumber())
                .serviceAreas(request.getServiceAreas() != null ? request.getServiceAreas() : List.of())
                .status(DriverStatus.PENDING_VERIFICATION)
                .availabilityStatus(AvailabilityStatus.OFFLINE)
                .build();

        DriverProfile saved = driverProfileRepository.save(profile);
        log.info("Driver profile created with ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public DriverProfileResponse getDriverProfileByAccountId(String accountId) {
        DriverProfile profile = driverProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public DriverProfileResponse getDriverProfileById(String driverId) {
        DriverProfile profile = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found with ID: " + driverId));
        return mapToResponse(profile);
    }

    @Transactional
    public DriverProfileResponse updateDriverProfile(
            String accountId, UpdateDriverProfileRequest request) {

        DriverProfile profile = driverProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        if (request.getFirstName() != null) profile.setFirstName(request.getFirstName());
        if (request.getLastName() != null) profile.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) profile.setPhoneNumber(request.getPhoneNumber());
        if (request.getServiceAreas() != null) {
            profile.getServiceAreas().clear();
            profile.getServiceAreas().addAll(request.getServiceAreas());
        }

        DriverProfile updated = driverProfileRepository.save(profile);
        log.info("Driver profile updated for account: {}", accountId);

        return mapToResponse(updated);
    }

    @Transactional
    public DriverProfileResponse updateAvailability(
            String accountId, UpdateAvailabilityRequest request) {

        DriverProfile profile = driverProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        if (profile.getStatus() != DriverStatus.ACTIVE) {
            throw new BusinessValidationException(
                    "Cannot update availability. Driver status is: " + profile.getStatus());
        }

        // Fetch vehicles from the repository to check for active vehicle
        if (request.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE) {
            List<Vehicle> activeVehicles = vehicleRepository.findByDriverIdAndIsActiveTrue(profile.getId());
            if (activeVehicles.isEmpty()) {
                throw new BusinessValidationException(
                        "Cannot set availability to AVAILABLE without an active vehicle");
            }
        }

        profile.setAvailabilityStatus(request.getAvailabilityStatus());
        DriverProfile updated = driverProfileRepository.save(profile);

        log.info("Driver availability updated to {} for account: {}",
                request.getAvailabilityStatus(), accountId);

        return mapToResponse(updated);
    }

    @Transactional
    public DriverProfileResponse updateLocation(
            String accountId, UpdateLocationRequest request) {

        DriverProfile profile = driverProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        profile.setCurrentLatitude(request.getLatitude());
        profile.setCurrentLongitude(request.getLongitude());
        if (request.getAddress() != null) {
            profile.setCurrentAddress(request.getAddress());
        }

        DriverProfile updated = driverProfileRepository.save(profile);
        log.info("Driver location updated for account: {}", accountId);

        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<DriverProfileResponse> getAllDrivers() {
        return driverProfileRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DriverProfileResponse updateDriverStatus(String driverId, DriverStatus newStatus) {
        DriverProfile profile = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        profile.setStatus(newStatus);

        if (newStatus == DriverStatus.SUSPENDED || newStatus == DriverStatus.DEACTIVATED) {
            profile.setAvailabilityStatus(AvailabilityStatus.OFFLINE);
        }

        DriverProfile updated = driverProfileRepository.save(profile);
        log.info("Driver status updated to {} for driver: {}", newStatus, driverId);

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteDriverProfile(String driverId) {
        DriverProfile profile = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        driverProfileRepository.delete(profile);
        log.info("Driver profile deleted: {}", driverId);
    }

    private DriverProfileResponse mapToResponse(DriverProfile profile) {
        // Fetch vehicles dynamically from the repository
        List<VehicleResponse> vehicles = vehicleRepository.findByDriverId(profile.getId()).stream()
                .map(this::mapVehicleToResponse)
                .collect(Collectors.toList());

        return DriverProfileResponse.builder()
                .id(profile.getId())
                .accountId(profile.getAccountId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .licenseNumber(profile.getLicenseNumber())
                .phoneNumber(profile.getPhoneNumber())
                .status(profile.getStatus())
                .availabilityStatus(profile.getAvailabilityStatus())
                .currentLatitude(profile.getCurrentLatitude())
                .currentLongitude(profile.getCurrentLongitude())
                .currentAddress(profile.getCurrentAddress())
                .serviceAreas(profile.getServiceAreas())
                .averageRating(profile.getAverageRating())
                .totalRides(profile.getTotalRides())
                .vehicles(vehicles)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private VehicleResponse mapVehicleToResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .registrationNumber(vehicle.getRegistrationNumber())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .color(vehicle.getColor())
                .vehicleType(vehicle.getVehicleType())
                .passengerCapacity(vehicle.getPassengerCapacity())
                .isActive(vehicle.getIsActive())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }
}