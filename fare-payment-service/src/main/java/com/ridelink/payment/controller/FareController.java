package com.ridelink.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ridelink.payment.dto.FareEstimateRequest;
import com.ridelink.payment.dto.FareEstimateResponse;
import com.ridelink.payment.dto.FareResponse;
import com.ridelink.payment.dto.FinalFareRequest;
import com.ridelink.payment.service.FareService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST endpoints for fare estimation and final fares.
 */
@RestController
@RequestMapping("/api/fares")
@Tag(name = "Fares", description = "Fare estimation and final fare calculation")
public class FareController {

    private final FareService fareService;

    public FareController(FareService fareService) {
        this.fareService = fareService;
    }

    @Operation(summary = "Estimate a fare",
            description = "Calculates the fare for a pickup and destination (known place names or "
                    + "\"lat,lon\" coordinates) using fare = baseFare + distanceKm x ratePerKm, with a "
                    + "minimum fare. Nothing is stored. No token needed: this is the internal call "
                    + "made by Ride Management.")
    @ApiResponse(responseCode = "200", description = "Estimate calculated")
    @ApiResponse(responseCode = "400", description = "Missing pickup or destination, or unknown location")
    @PostMapping("/estimate")
    public FareEstimateResponse estimate(@Valid @RequestBody FareEstimateRequest request) {
        return fareService.estimate(request);
    }

    @Operation(summary = "Calculate and store the final fare of a ride",
            description = "Called when a ride is completed. Provide actualDistanceKm, or both pickup and "
                    + "destination to calculate the distance. One final fare per ride. Roles: DRIVER, ADMIN.")
    @ApiResponse(responseCode = "201", description = "Final fare stored")
    @ApiResponse(responseCode = "400", description = "Invalid input (blank ids, negative distance, unknown location, or no distance/locations given)")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    @ApiResponse(responseCode = "403", description = "Role not allowed")
    @ApiResponse(responseCode = "409", description = "A final fare already exists for this ride")
    @PostMapping("/final")
    @ResponseStatus(HttpStatus.CREATED)
    public FareResponse calculateFinalFare(@Valid @RequestBody FinalFareRequest request) {
        return fareService.calculateFinalFare(request);
    }

    @Operation(summary = "Get the stored final fare of a ride", description = "Any authenticated user.")
    @ApiResponse(responseCode = "200", description = "Fare found")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    @ApiResponse(responseCode = "404", description = "No fare stored for this ride")
    @GetMapping("/ride/{rideId}")
    public FareResponse getFareByRideId(@PathVariable("rideId") String rideId) {
        return fareService.getFareByRideId(rideId);
    }
}
