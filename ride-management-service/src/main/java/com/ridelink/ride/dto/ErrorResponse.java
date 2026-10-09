package com.ridelink.ride.dto;

import java.time.LocalDateTime;

public class ErrorResponse {

    private String error;
    private String message;
    private LocalDateTime timestamp;

    public ErrorResponse() {
    }

    public ErrorResponse(
            String error,
            String message,
            LocalDateTime timestamp) {

        this.error = error;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}