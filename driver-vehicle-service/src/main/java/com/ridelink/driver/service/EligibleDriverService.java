package com.ridelink.driver.service;

import com.ridelink.driver.dto.response.EligibleDriverResponse;
import com.ridelink.driver.entity.DriverProfile;
import com.ridelink.driver.entity.Vehicle;
import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.enums.DriverStatus;
import com.ridelink.driver.enums.VehicleType;
import com.ridelink.driver.repository.DriverProfileRepository;
import com.ridelink.driver.repository.VehicleRepository;
import com.ridelink.driver.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EligibleDriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;

    private static final double DEFAULT_SEARCH_RADIUS_KM = 10.0;

    @Transactional(readOnly = true)
    public List<EligibleDriverResponse> findEligibleDrivers(
            Double pickupLatitude,
            Double pickupLongitude,
            VehicleType vehicleType,
            Integer requiredCapacity,
            Double searchRadiusKm) {

        log.info("Finding eligible drivers - pickup: ({}, {}), vehicleType: {}, capacity: {}",
                pickupLatitude, pickupLongitude, vehicleType, requiredCapacity);

        double radius = searchRadiusKm != null ? searchRadiusKm : DEFAULT_SEARCH_RADIUS_KM;

        // 1. Get all available drivers
        List<DriverProfile> availableDrivers = driverProfileRepository.findAllAvailableDrivers();

        return availableDrivers.stream()
                // 2. Fetch their vehicles and filter based on requirements
                .filter(driver -> {
                    List<Vehicle> activeVehicles = vehicleRepository.findByDriverIdAndIsActiveTrue(driver.getId());
                    return !activeVehicles.isEmpty() && hasMatchingVehicle(activeVehicles, vehicleType, requiredCapacity);
                })
                // 3. Map to response with distance calculation
                .map(driver -> {
                    List<Vehicle> activeVehicles = vehicleRepository.findByDriverIdAndIsActiveTrue(driver.getId());
                    return mapToEligibleDriver(driver, activeVehicles, pickupLatitude, pickupLongitude);
                })
                // 4. Filter by search radius
                .filter(response -> response.getDistanceKm() == null
                        || response.getDistanceKm() <= radius)
                // 5. Sort by nearest driver
                .sorted(Comparator.comparing(
                        EligibleDriverResponse::getDistanceKm,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EligibleDriverResponse> findEligibleDriversByServiceArea(
            List<String> serviceAreas,
            VehicleType vehicleType,
            Integer requiredCapacity) {

        log.info("Finding eligible drivers in service areas: {}", serviceAreas);

        List<DriverProfile> drivers = driverProfileRepository.findByServiceAreas(serviceAreas);

        return drivers.stream()
                .filter(driver -> driver.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE)
                .filter(driver -> driver.getStatus() == DriverStatus.ACTIVE)
                .filter(driver -> {
                    List<Vehicle> activeVehicles = vehicleRepository.findByDriverIdAndIsActiveTrue(driver.getId());
                    return !activeVehicles.isEmpty() && hasMatchingVehicle(activeVehicles, vehicleType, requiredCapacity);
                })
                .map(driver -> {
                    List<Vehicle> activeVehicles = vehicleRepository.findByDriverIdAndIsActiveTrue(driver.getId());
                    return mapToEligibleDriver(driver, activeVehicles, null, null);
                })
                .collect(Collectors.toList());
    }

    private boolean hasMatchingVehicle(
            List<Vehicle> vehicles, VehicleType vehicleType, Integer requiredCapacity) {

        return vehicles.stream()
                .anyMatch(v -> {
                    boolean typeMatches = vehicleType == null || v.getVehicleType() == vehicleType;
                    boolean capacityMatches = requiredCapacity == null
                            || v.getPassengerCapacity() >= requiredCapacity;
                    return typeMatches && capacityMatches;
                });
    }

    private EligibleDriverResponse mapToEligibleDriver(
            DriverProfile driver, List<Vehicle> activeVehicles, Double pickupLat, Double pickupLon) {

        Double distance = null;
        if (pickupLat != null && pickupLon != null
                && driver.getCurrentLatitude() != null && driver.getCurrentLongitude() != null) {
            distance = GeoUtils.calculateDistance(
                    pickupLat, pickupLon,
                    driver.getCurrentLatitude(), driver.getCurrentLongitude());
        }

        List<EligibleDriverResponse.VehicleSummary> vehicleSummaries = activeVehicles.stream()
                .map(this::mapVehicleSummary)
                .collect(Collectors.toList());

        String primaryVehicleType = activeVehicles.stream()
                .findFirst()
                .map(v -> v.getVehicleType().name())
                .orElse(null);

        return EligibleDriverResponse.builder()
                .driverId(driver.getId())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .phoneNumber(driver.getPhoneNumber())
                .averageRating(driver.getAverageRating())
                .totalRides(driver.getTotalRides())
                .currentLatitude(driver.getCurrentLatitude())
                .currentLongitude(driver.getCurrentLongitude())
                .currentAddress(driver.getCurrentAddress())
                .distanceKm(distance != null ? Math.round(distance * 100.0) / 100.0 : null)
                .vehicles(vehicleSummaries)
                .primaryVehicleType(primaryVehicleType)
                .build();
    }

    private EligibleDriverResponse.VehicleSummary mapVehicleSummary(Vehicle vehicle) {
        return EligibleDriverResponse.VehicleSummary.builder()
                .vehicleId(vehicle.getId())
                .registrationNumber(vehicle.getRegistrationNumber())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .color(vehicle.getColor())
                .vehicleType(vehicle.getVehicleType())
                .passengerCapacity(vehicle.getPassengerCapacity())
                .build();
    }
}