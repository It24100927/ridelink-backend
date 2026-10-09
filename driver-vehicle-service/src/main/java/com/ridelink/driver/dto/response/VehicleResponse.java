package com.ridelink.driver.dto.response;

import com.ridelink.driver.enums.VehicleType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {

    private String id;
    private String registrationNumber;
    private String make;
    private String model;
    private Integer year;
    private String color;
    private VehicleType vehicleType;
    private Integer passengerCapacity;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}