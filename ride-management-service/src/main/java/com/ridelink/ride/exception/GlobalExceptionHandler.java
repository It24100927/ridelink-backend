package com.ridelink.ride.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ridelink.ride.dto.ErrorResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RideNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRideNotFound(
            RideNotFoundException exception) {

        ErrorResponse error = new ErrorResponse(
                "Ride Not Found",
                exception.getMessage(),
                LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(InvalidRideStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRideState(
            InvalidRideStateException exception) {

        ErrorResponse error = new ErrorResponse(
                "Invalid Ride State",
                exception.getMessage(),
                LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(DriverNotAssignedException.class)
    public ResponseEntity<ErrorResponse> handleDriverNotAssigned(
            DriverNotAssignedException exception) {

        ErrorResponse error = new ErrorResponse(
                "Driver Not Assigned",
                exception.getMessage(),
                LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(error);
    }

    @ExceptionHandler(NoAvailableDriverException.class)
    public ResponseEntity<ErrorResponse> handleNoAvailableDriver(
            NoAvailableDriverException exception) {

        ErrorResponse error = new ErrorResponse(
                "No Available Driver",
                exception.getMessage(),
                LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalService(
            ExternalServiceException exception) {

        ErrorResponse error = new ErrorResponse(
                "External Service Error",
                exception.getMessage(),
                LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException exception) {

        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(
                        error.getField(),
                        error.getDefaultMessage()));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }
}