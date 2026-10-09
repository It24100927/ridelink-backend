package com.ridelink.account.security;

import com.ridelink.account.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

import java.util.Set;

/** Small helper controllers call to enforce requireAuth() / requireRole() semantics. */
public final class AuthHelper {

    private AuthHelper() { }

    public static AuthenticatedUser requireAuth(HttpServletRequest request) {
        Object attr = request.getAttribute(JwtAuthFilter.AUTH_USER_ATTR);
        if (attr == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        return (AuthenticatedUser) attr;
    }

    public static String requireToken(HttpServletRequest request) {
        Object token = request.getAttribute(JwtAuthFilter.RAW_TOKEN_ATTR);
        if (token == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        return (String) token;
    }

    public static void requireRole(AuthenticatedUser user, String... roles) {
        if (!Set.of(roles).contains(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden: insufficient role");
        }
    }
}
