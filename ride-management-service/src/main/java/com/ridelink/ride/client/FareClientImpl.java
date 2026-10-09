package com.ridelink.ride.client;

import com.ridelink.ride.dto.FareEstimateRequest;
import com.ridelink.ride.dto.FareEstimateResponse;
import com.ridelink.ride.exception.ExternalServiceException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class FareClientImpl implements FareClient {

    private final RestClient fareRestClient;

    public FareClientImpl(RestClient fareRestClient) {
        this.fareRestClient = fareRestClient;
    }

    @Override
    public FareEstimateResponse estimateFare(
            FareEstimateRequest request) {

        try {

            FareEstimateResponse response =
                    fareRestClient.post()
                            .uri("/api/fares/estimate")
                            .body(request)
                            .retrieve()
                            .body(FareEstimateResponse.class);

            if (response == null) {
                throw new ExternalServiceException(
                        "Fare & Payment Service returned an empty response"
                );
            }

            return response;

        } catch (RestClientException exception) {

            throw new ExternalServiceException(
                    "Unable to communicate with Fare & Payment Service",
                    exception
            );
        }
    }
}