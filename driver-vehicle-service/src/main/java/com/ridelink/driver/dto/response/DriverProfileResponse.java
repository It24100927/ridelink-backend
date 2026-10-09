package com.ridelink.driver.dto.response;

import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.enums.DriverStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverProfileResponse {

    private String id;
    private String accountId;
    private String firstName;
    private String lastName;
    private String licenseNumber;
    private String phoneNumber;
    private DriverStatus status;
    private AvailabilityStatus availabilityStatus;
    private Double currentLatitude;
    private Double currentLongitude;
    private String currentAddress;
    private List<String> serviceAreas;
    private Double averageRating;
    private Integer totalRides;
    private List<VehicleResponse> vehicles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}