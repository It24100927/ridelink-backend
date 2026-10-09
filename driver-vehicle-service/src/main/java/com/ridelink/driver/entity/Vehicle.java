package com.ridelink.driver.entity;

import com.ridelink.driver.enums.VehicleType;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    private String id;

    private String driverId; // Changed from DriverProfile driver to String driverId

    private String registrationNumber;
    private String make;
    private String model;
    private Integer year;
    private String color;
    private VehicleType vehicleType;
    private Integer passengerCapacity;

    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}