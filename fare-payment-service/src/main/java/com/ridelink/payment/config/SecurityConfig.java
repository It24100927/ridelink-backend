package com.ridelink.payment.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Authentication and role-based authorisation.
 *
 * Tokens are JWTs issued by the Account Service and signed with a shared HS256 secret.
 * This service only validates tokens; it never issues them.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // Internal read-only call from Ride Management, which sends no token; stores nothing.
                        .requestMatchers(HttpMethod.POST, "/api/fares/estimate").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/fares/final").hasAnyRole("DRIVER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/payments").hasAnyRole("PASSENGER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/fares/**", "/api/payments/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                                        "Authentication is required"))
                        .accessDeniedHandler((request, response, e) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                                        "Your role is not allowed to perform this operation")))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint((request, response, e) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                                        "Authentication is required"))
                        .accessDeniedHandler((request, response, e) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                                        "Your role is not allowed to perform this operation")));
        return http.build();
    }

    /** Verifies the token signature with the shared secret (JWT_SECRET environment variable). */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Maps the token's roles claim (e.g. ["PASSENGER"]) to Spring authorities (ROLE_PASSENGER). */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(
            @Value("${security.jwt.roles-claim:roles}") String rolesClaim) {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(rolesClaim);
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /** Writes a 401/403 body in the same JSON shape as every other error of this service. */
    private static void writeError(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":" + status
                + ",\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }
}
