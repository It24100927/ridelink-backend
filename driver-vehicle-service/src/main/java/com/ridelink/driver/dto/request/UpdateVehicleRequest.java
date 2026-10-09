package com.ridelink.driver.dto.request;

import com.ridelink.driver.enums.VehicleType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVehicleRequest {

    @Size(max = 50, message = "Make must not exceed 50 characters")
    private String make;

    @Size(max = 50, message = "Model must not exceed 50 characters")
    private String model;

    @Min(value = 1900, message = "Year must be after 1900")
    @Max(value = 2100, message = "Year must be before 2100")
    private Integer year;

    @Size(max = 30, message = "Color must not exceed 30 characters")
    private String color;

    private VehicleType vehicleType;

    @Min(value = 1, message = "Passenger capacity must be at least 1")
    @Max(value = 20, message = "Passenger capacity must not exceed 20")
    private Integer passengerCapacity;

    private Boolean isActive;
}
