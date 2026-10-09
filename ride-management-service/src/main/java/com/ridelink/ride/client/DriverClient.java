package com.ridelink.ride.client;

import com.ridelink.ride.dto.AvailableDriverResponse;

import java.util.List;

public interface DriverClient {

    List<AvailableDriverResponse> getEligibleDrivers();
}