package com.ridelink.ride.dto; 

import jakarta.validation.constraints.NotBlank;

public class CreateRideRequest {

    @NotBlank(message = "Passenger ID is required")
    private String passengerId;

    @NotBlank(message = "Pickup location is required")
    private String pickupLocation;

    @NotBlank(message = "Destination is required")
    private String destination;

    public CreateRideRequest(){

    }

    public String getPassengerId(){
        return passengerId;
    }

    public void setPassengerId(String passengerId){
        this.passengerId=passengerId;
    }

    public String getPickupLocation(){
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation){
        this.pickupLocation=pickupLocation;
    }

    public String getDestination(){
        return destination;
    }

    public void setDestination(String destination){
        this.destination=destination;
    }
    
}
