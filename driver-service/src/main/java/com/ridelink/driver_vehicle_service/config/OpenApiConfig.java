package com.ridelink.driver_vehicle_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configures springdoc-openapi with:
 *  - API metadata (title, version, description)
 *  - A Bearer JWT security scheme so Swagger UI shows the padlock on
 *    every protected endpoint that carries @SecurityRequirement("bearerAuth").
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title       = "RideLink – Driver & Vehicle Service",
                version     = "1.0",
                description = "Manages driver profiles and vehicle registrations. "
                            + "All mutating endpoints require a Bearer JWT issued by the Account Service."
        )
)
@SecurityScheme(
        name   = "bearerAuth",
        type   = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in     = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
    // configuration is fully declarative via annotations
}
