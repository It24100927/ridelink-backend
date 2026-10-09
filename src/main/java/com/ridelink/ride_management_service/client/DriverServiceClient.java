package com.ridelink.ride_management_service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
* Driver & Vehicle Service REST Client
* Call to get a list of eligible drivers
 */
@Component
@RequiredArgsConstructor
public class DriverServiceClient {

    private final RestTemplate restTemplate;

    @Value("${driver.service.url:http://localhost:8082}")
    private String driverServiceUrl;

    /**
     * Gets available drivers list (serviceArea filter) 
     * @return List of driver info maps (driverId included)
    */
    public List<Map<String, Object>> getEligibleDrivers(String serviceArea) {
        String url = driverServiceUrl + "/api/drivers/eligible?serviceArea=" + serviceArea;
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }
}
