package com.ridelink.ride_management_service.model;

import com.ridelink.ride_management_service.enums.RideStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Document(collection = "rides")
public class Ride {

    @Id
    private String rideId;

    private String passengerId;
    private String driverId;

    private String pickup;
    private String destination;
    private String serviceArea;

    private Double distanceKm;

    private RideStatus status;

    // Lifecycle timestamps
    private LocalDateTime assignedAt;

    private String finalFareId;

    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
}
