package com.ridelink.ride.client;

import com.ridelink.ride.dto.FareEstimateRequest;
import com.ridelink.ride.dto.FareEstimateResponse;

public interface FareClient {

    FareEstimateResponse estimateFare(FareEstimateRequest request);
}