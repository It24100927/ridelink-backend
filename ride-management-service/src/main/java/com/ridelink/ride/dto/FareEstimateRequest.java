package com.ridelink.ride.dto;

public class FareEstimateRequest {

    private String pickup;
    private String destination;

    public FareEstimateRequest() {
    }

    public FareEstimateRequest(String pickup, String destination) {
        this.pickup = pickup;
        this.destination = destination;
    }

    public String getPickup() {
        return pickup;
    }

    public void setPickup(String pickup) {
        this.pickup = pickup;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}