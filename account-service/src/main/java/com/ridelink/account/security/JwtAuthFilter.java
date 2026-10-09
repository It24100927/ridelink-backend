package com.ridelink.account.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Verifies the JWT issued by account-service (all services trust it because they
 * share the same jwt.secret via each service's own application.yml / environment
 * variable - no plaintext secret is committed, and no service calls account-service
 * on every request just to check a token).
 *
 * On success, stores an AuthenticatedUser on the request as an attribute so
 * controllers can read req.getAttribute("authUser"). Leaves the request to fail
 * with 401/403 inside individual controllers/services if the attribute is missing -
 * this mirrors the requireAuth / requireRole middleware pattern.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String AUTH_USER_ATTR = "authUser";
    public static final String RAW_TOKEN_ATTR = "authToken";

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        // Allow health checks and Swagger without a token
        if (path.startsWith("/health") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                var claims = jwtUtil.parseClaims(token);
                AuthenticatedUser user = new AuthenticatedUser(
                        claims.getSubject(),
                        claims.get("role", String.class),
                        claims.get("email", String.class)
                );
                request.setAttribute(AUTH_USER_ATTR, user);
                request.setAttribute(RAW_TOKEN_ATTR, token);
            } catch (JwtException ignored) {
                // leave authUser unset - downstream will reject with 401 if auth is required
            }
        }
        filterChain.doFilter(request, response);
    }
}
