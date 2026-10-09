package lk.ridelink.common;
import io.swagger.v3.oas.models.OpenAPI;import io.swagger.v3.oas.models.Components;import io.swagger.v3.oas.models.info.Info;import io.swagger.v3.oas.models.security.SecurityScheme;import org.springframework.context.annotation.Bean;import org.springframework.context.annotation.Configuration;
@Configuration public class OpenApiConfiguration {
 @Bean OpenAPI rideLinkOpenApi(){return new OpenAPI().info(new Info().title("RideLink Service API").version("1.0.0").description("Backend API for the IT3130 RideLink assignment.")).components(new Components().addSecuritySchemes("bearerAuth",new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));}
}
