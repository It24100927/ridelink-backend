package com.ridelink.ride.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient driverRestClient(
            @Value("${driver.service.url}") String driverServiceUrl) {

        return RestClient.builder()
                .baseUrl(driverServiceUrl)
                .build();
    }

    @Bean
    public RestClient fareRestClient(
            @Value("${fare.service.url}") String fareServiceUrl) {

        return RestClient.builder()
                .baseUrl(fareServiceUrl)
                .build();
    }
}