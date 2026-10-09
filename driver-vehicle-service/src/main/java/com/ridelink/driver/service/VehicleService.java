package com.ridelink.driver.service;

import com.ridelink.driver.dto.request.CreateVehicleRequest;
import com.ridelink.driver.dto.request.UpdateVehicleRequest;
import com.ridelink.driver.dto.response.VehicleResponse;
import com.ridelink.driver.entity.DriverProfile;
import com.ridelink.driver.entity.Vehicle;
import com.ridelink.driver.exception.BusinessValidationException;
import com.ridelink.driver.exception.ResourceNotFoundException;
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
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverProfileRepository driverProfileRepository;

    @Transactional
    public VehicleResponse createVehicle(String accountId, CreateVehicleRequest request) {
        DriverProfile driver = driverProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        if (vehicleRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new BusinessValidationException("Vehicle with this registration number already exists");
        }

        Vehicle vehicle = Vehicle.builder()
                .driverId(driver.getId())
                .registrationNumber(request.getRegistrationNumber())
                .make(request.getMake())
                .model(request.getModel())
                .year(request.getYear())
                .color(request.getColor())
                .vehicleType(request.getVehicleType())
                .passengerCapacity(request.getPassengerCapacity())
                .isActive(true)
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle created with ID: {} for driver: {}", saved.getId(), driver.getId());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehiclesByAccountId(String accountId) {
        DriverProfile driver = driverProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        return vehicleRepository.findByDriverId(driver.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(String accountId, String vehicleId) {
        DriverProfile driver = driverProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        Vehicle vehicle = vehicleRepository.findByIdAndDriverId(vehicleId, driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        return mapToResponse(vehicle);
    }

    @Transactional
    public VehicleResponse updateVehicle(
            String accountId, String vehicleId, UpdateVehicleRequest request) {

        DriverProfile driver = driverProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        Vehicle vehicle = vehicleRepository.findByIdAndDriverId(vehicleId, driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        if (request.getMake() != null) vehicle.setMake(request.getMake());
        if (request.getModel() != null) vehicle.setModel(request.getModel());
        if (request.getYear() != null) vehicle.setYear(request.getYear());
        if (request.getColor() != null) vehicle.setColor(request.getColor());
        if (request.getVehicleType() != null) vehicle.setVehicleType(request.getVehicleType());
        if (request.getPassengerCapacity() != null) vehicle.setPassengerCapacity(request.getPassengerCapacity());
        if (request.getIsActive() != null) vehicle.setIsActive(request.getIsActive());

        Vehicle updated = vehicleRepository.save(vehicle);
        log.info("Vehicle updated: {}", vehicleId);

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteVehicle(String accountId, String vehicleId) {
        DriverProfile driver = driverProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        Vehicle vehicle = vehicleRepository.findByIdAndDriverId(vehicleId, driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        vehicleRepository.delete(vehicle);
        log.info("Vehicle deleted: {}", vehicleId);
    }

    private VehicleResponse mapToResponse(Vehicle vehicle) {
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