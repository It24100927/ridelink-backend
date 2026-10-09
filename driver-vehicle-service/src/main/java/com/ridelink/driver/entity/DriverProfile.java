package com.ridelink.driver.entity;

import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.enums.DriverStatus;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverProfile {

    @Id
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

    @Builder.Default
    private List<String> serviceAreas = new ArrayList<>();

    @Builder.Default
    private Double averageRating = 5.0;

    @Builder.Default
    private Integer totalRides = 0;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}