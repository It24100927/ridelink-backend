package com.ridelink.driver.dto.response;

import com.ridelink.driver.enums.VehicleType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibleDriverResponse {

    private String driverId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Double averageRating;
    private Integer totalRides;
    private Double currentLatitude;
    private Double currentLongitude;
    private String currentAddress;
    private Double distanceKm; // Distance from pickup point if provided
    private List<VehicleSummary> vehicles;
    private String primaryVehicleType;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleSummary {
        private String vehicleId;
        private String registrationNumber;
        private String make;
        private String model;
        private String color;
        private VehicleType vehicleType;
        private Integer passengerCapacity;
    }
}