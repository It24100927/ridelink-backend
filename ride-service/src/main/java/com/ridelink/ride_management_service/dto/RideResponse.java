package com.ridelink.ride_management_service.dto;

import com.ridelink.ride_management_service.enums.RideStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RideResponse {
    private String rideId;
    private String passengerId;
    private String driverId;
    private String pickup;
    private String destination;
    private String serviceArea;
    private Double distanceKm;
    private RideStatus status;
    private String finalFareId;
    private LocalDateTime createdAt;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
}
