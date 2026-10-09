package com.ridelink.ride.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rideManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RideLink Ride Management Service API")
                        .version("1.0")
                        .description(
                                "REST API for managing RideLink ride requests, "
                                        + "driver assignment, ride lifecycle, "
                                        + "and ride retrieval."));
    }
}