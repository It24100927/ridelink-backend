package com.ridelink.driver.dto.request;

import com.ridelink.driver.enums.VehicleType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVehicleRequest {

    @NotBlank(message = "Registration number is required")
    @Pattern(regexp = "^[A-Z0-9-]{2,10}$", message = "Registration number must be 2-10 alphanumeric characters")
    private String registrationNumber;

    @NotBlank(message = "Make is required")
    @Size(max = 50, message = "Make must not exceed 50 characters")
    private String make;

    @NotBlank(message = "Model is required")
    @Size(max = 50, message = "Model must not exceed 50 characters")
    private String model;

    @NotNull(message = "Year is required")
    @Min(value = 1900, message = "Year must be after 1900")
    @Max(value = 2100, message = "Year must be before 2100")
    private Integer year;

    @NotBlank(message = "Color is required")
    @Size(max = 30, message = "Color must not exceed 30 characters")
    private String color;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotNull(message = "Passenger capacity is required")
    @Min(value = 1, message = "Passenger capacity must be at least 1")
    @Max(value = 20, message = "Passenger capacity must not exceed 20")
    private Integer passengerCapacity;
}