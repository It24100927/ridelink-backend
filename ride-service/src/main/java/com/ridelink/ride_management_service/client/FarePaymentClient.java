package com.ridelink.ride_management_service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Fare & Payment Service REST Client
 * Called to create final fare after ride is complete
 */
@Component
@RequiredArgsConstructor
public class FarePaymentClient {

    private final RestTemplate restTemplate;

    @Value("${fare.service.url:http://localhost:8084}")
    private String fareServiceUrl;

    /**
     * Final fare is created after ride completion
     * @return fareId (String)
    */
    public String createFinalFare(String rideId, Double distanceKm) {
        String url = fareServiceUrl + "/api/fares/final";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "rideId", rideId,
                "distanceKm", distanceKm
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

        if (response != null && response.containsKey("fareId")) {
            return (String) response.get("fareId");
        }
        return null;
    }
}
