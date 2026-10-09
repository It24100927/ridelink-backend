package com.ridelink.payment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.ridelink.payment.config.FareProperties;
import com.ridelink.payment.dto.FareEstimateRequest;
import com.ridelink.payment.dto.FareEstimateResponse;
import com.ridelink.payment.dto.FareResponse;
import com.ridelink.payment.dto.FinalFareRequest;
import com.ridelink.payment.exception.ConflictException;
import com.ridelink.payment.exception.InvalidRequestException;
import com.ridelink.payment.exception.ResourceNotFoundException;
import com.ridelink.payment.model.Fare;
import com.ridelink.payment.repository.FareRepository;

/**
 * Fare estimation and final fare recording.
 */
@Service
public class FareService {

    private final FareRepository fareRepository;
    private final FareCalculationService calculationService;
    private final DistanceCalculator distanceCalculator;
    private final FareProperties properties;
    private final LocationResolver locationResolver;

    public FareService(FareRepository fareRepository,
                       FareCalculationService calculationService,
                       DistanceCalculator distanceCalculator,
                       FareProperties properties,
                       LocationResolver locationResolver) {
        this.fareRepository = fareRepository;
        this.calculationService = calculationService;
        this.distanceCalculator = distanceCalculator;
        this.properties = properties;
        this.locationResolver = locationResolver;
    }

    /**
     * Estimates a fare from a pickup and a destination (place names or "lat,lon"). Nothing is saved.
     *
     * @throws InvalidRequestException if a location is unknown or invalid
     */
    public FareEstimateResponse estimate(FareEstimateRequest request) {
        BigDecimal distanceKm = distanceBetween(request.pickup(), request.destination());

        return new FareEstimateResponse(
                request.pickup().trim(),
                request.destination().trim(),
                distanceKm,
                properties.getBaseFare(),
                properties.getRatePerKm(),
                properties.getMinimumFare(),
                calculationService.calculate(distanceKm),
                properties.getCurrency());
    }

    /**
     * Calculates and stores the final fare for a completed ride.
     *
     * @throws InvalidRequestException if neither actualDistanceKm nor both locations are given
     * @throws ConflictException       if a fare already exists for this ride
     */
    public FareResponse calculateFinalFare(FinalFareRequest request) {
        BigDecimal distanceKm = resolveFinalDistance(request);

        if (fareRepository.existsByRideId(request.rideId())) {
            throw duplicateFare(request.rideId());
        }

        Fare fare = new Fare();
        fare.setRideId(request.rideId());
        fare.setPassengerId(request.passengerId());
        fare.setDistanceKm(distanceKm);
        fare.setBaseFare(properties.getBaseFare());
        fare.setRatePerKm(properties.getRatePerKm());
        fare.setTotalFare(calculationService.calculate(distanceKm));
        fare.setCurrency(properties.getCurrency());
        fare.setCalculatedAt(Instant.now());

        try {
            return FareResponse.from(fareRepository.save(fare));
        } catch (DuplicateKeyException ex) {
            // Concurrent request won the race; the unique index on rideId rejected this one.
            throw duplicateFare(request.rideId());
        }
    }

    /**
     * @throws ResourceNotFoundException if no fare is stored for this ride
     */
    public FareResponse getFareByRideId(String rideId) {
        Fare fare = fareRepository.findByRideId(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("No fare found for ride " + rideId));
        return FareResponse.from(fare);
    }

    private BigDecimal resolveFinalDistance(FinalFareRequest request) {
        if (request.actualDistanceKm() != null) {
            return request.actualDistanceKm().setScale(2, RoundingMode.HALF_UP);
        }
        if (hasText(request.pickup()) && hasText(request.destination())) {
            return distanceBetween(request.pickup(), request.destination());
        }
        throw new InvalidRequestException(
                "Provide actualDistanceKm, or both pickup and destination, to calculate the final fare");
    }

    private BigDecimal distanceBetween(String pickup, String destination) {
        LocationResolver.Coordinates from = locationResolver.resolve(pickup);
        LocationResolver.Coordinates to = locationResolver.resolve(destination);
        return distanceCalculator.distanceKm(from.latitude(), from.longitude(), to.latitude(), to.longitude());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ConflictException duplicateFare(String rideId) {
        return new ConflictException("A final fare already exists for ride " + rideId);
    }
}
