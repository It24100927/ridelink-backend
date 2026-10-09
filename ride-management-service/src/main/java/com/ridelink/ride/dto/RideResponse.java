package com.ridelink.ride.dto;

import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RideResponse {

    private String rideId;
    private String passengerId;
    private String driverId;
    private String pickupLocation;
    private String destination;
    private RideStatus status;
    private BigDecimal estimatedFare;
    private BigDecimal finalFare;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RideResponse() {

    }

    public RideResponse(Ride ride) {
        this.rideId = ride.getId();
        this.passengerId = ride.getPassengerId();
        this.driverId = ride.getDriverId();
        this.pickupLocation = ride.getPickupLocation();
        this.destination = ride.getDestination();
        this.status = ride.getStatus();
        this.estimatedFare = ride.getEstimatedFare();
        this.finalFare = ride.getFinalFare();
        this.createdAt = ride.getCreatedAt();
        this.updatedAt = ride.getUpdatedAt();
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(String passengerId) {
        this.passengerId = passengerId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
        this.status = status;
    }

    public BigDecimal getEstimatedFare() {
        return estimatedFare;
    }

    public void setEstimatedFare(BigDecimal estimatedFare) {
        this.estimatedFare = estimatedFare;
    }

    public BigDecimal getFinalFare() {
        return finalFare;
    }

    public void setFinalFare(BigDecimal finalFare) {
        this.finalFare = finalFare;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
