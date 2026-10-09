package com.ridelink.ride_management_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateRideRequest {

    @NotBlank(message = "Pickup location is required")
    @Size(max = 100, message = "Pickup location must be at most 100 characters")
    private String pickup;

    @NotBlank(message = "Destination is required")
    @Size(max = 100, message = "Destination must be at most 100 characters")
    private String destination;

    @NotBlank(message = "Service area is required")
    @Size(max = 100, message = "Service area must be at most 100 characters")
    private String serviceArea;

    @NotNull(message = "Distance is required")
    @PositiveOrZero(message = "Distance must be 0 or greater")
    private Double distanceKm;
}
