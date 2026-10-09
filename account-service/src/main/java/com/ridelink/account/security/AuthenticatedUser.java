package com.ridelink.account.security;

public class AuthenticatedUser {
    private final String id;
    private final String role;
    private final String email;

    public AuthenticatedUser(String id, String role, String email) {
        this.id = id;
        this.role = role;
        this.email = email;
    }

    public String getId() { return id; }
    public String getRole() { return role; }
    public String getEmail() { return email; }
}
