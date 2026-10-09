package com.ridelink.ride.client;

import com.ridelink.ride.dto.AvailableDriverResponse;
import com.ridelink.ride.exception.ExternalServiceException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class DriverClientImpl implements DriverClient {

    private final RestClient driverRestClient;

    public DriverClientImpl(RestClient driverRestClient) {
        this.driverRestClient = driverRestClient;
    }

    @Override
    public List<AvailableDriverResponse> getEligibleDrivers() {

        try {
            List<AvailableDriverResponse> drivers =
                    driverRestClient.get()
                            .uri("/api/drivers/eligible")
                            .retrieve()
                            .body(
                                    new ParameterizedTypeReference<
                                            List<AvailableDriverResponse>>() {
                                    }
                            );

            return drivers != null ? drivers : List.of();

        } catch (RestClientException exception) {

            throw new ExternalServiceException(
                    "Unable to communicate with Driver & Vehicle Service",
                    exception
            );
        }
    }
}