package com.ridelink.driver.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ===== Public Endpoints =====
                        // Swagger / OpenAPI
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // Actuator (all sub-paths)
                        .requestMatchers("/actuator/**").permitAll()
                        // Error endpoint (Spring Boot default)
                        .requestMatchers("/error").permitAll()

                        // ===== Internal Service-to-Service Endpoints =====
                        // Called by Ride Management Service (Sadinsa) — must match your controller path
                        .requestMatchers(HttpMethod.GET, "/api/drivers/eligible").permitAll()

                        // ===== Driver Profile Endpoints (authenticated DRIVER) =====
                        .requestMatchers(HttpMethod.POST,   "/api/drivers").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.GET,    "/api/drivers/me").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.PUT,    "/api/drivers/me").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.PATCH,  "/api/drivers/me/availability").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.PATCH,  "/api/drivers/me/location").hasRole("DRIVER")

                        // ===== Admin Endpoints =====
                        .requestMatchers(HttpMethod.GET,    "/api/drivers").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,    "/api/drivers/{id}").hasAnyRole("ADMIN", "DRIVER")
                        .requestMatchers(HttpMethod.PATCH,  "/api/drivers/{id}/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/drivers/{id}").hasRole("ADMIN")

                        // ===== Vehicle Endpoints (authenticated DRIVER) =====
                        .requestMatchers(HttpMethod.POST,   "/api/drivers/me/vehicles").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.GET,    "/api/drivers/me/vehicles").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.GET,    "/api/drivers/me/vehicles/{vehicleId}").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.PUT,    "/api/drivers/me/vehicles/{vehicleId}").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.DELETE, "/api/drivers/me/vehicles/{vehicleId}").hasRole("DRIVER")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}