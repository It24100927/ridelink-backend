package com.ridelink.ride_management_service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Account Service REST Client
 *
 * Note: account-service exposes:
 *   GET /api/accounts/me  — returns profile of the authenticated user (JWT required)
 *   PATCH /api/accounts/{id}/status — admin only
 *
 * There is no public GET-by-accountId endpoint on account-service.
 * If ride-service needs to look up a specific user, the JWT of that user
 * must be forwarded, or a dedicated internal endpoint must be added to account-service.
 *
 * Current usage: reserved for future use / internal lookups.
 */
@Component
@RequiredArgsConstructor
public class AccountServiceClient {

    private final RestTemplate restTemplate;

    @Value("${account.service.url:http://localhost:8081}")
    private String accountServiceUrl;

    /**
     * Fetch own profile from account-service by forwarding the caller's JWT.
     * Calls GET /api/accounts/me on the account-service.
     *
     * @param bearerToken  the raw JWT (without "Bearer " prefix)
     * @return Map of user profile fields
     */
    public Map<String, Object> getOwnProfile(String bearerToken) {
        String url = accountServiceUrl + "/api/accounts/me";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + bearerToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class
        );
        return response.getBody();
    }
}