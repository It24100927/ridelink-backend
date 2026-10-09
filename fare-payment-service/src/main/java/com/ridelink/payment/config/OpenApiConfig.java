package com.ridelink.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger / OpenAPI metadata. Swagger UI: http://localhost:8084/swagger-ui/index.html
 * Adds a Bearer-token "Authorize" button so secured endpoints can be tried from Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI fareAndPaymentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("RideLink - Fare & Payment Service")
                        .version("1.0.0")
                        .description("Fare estimation, final fare calculation, simulated payments and receipts. "
                                + "All payments are simulated; no real payment gateway is used."))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
