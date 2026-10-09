package com.ridelink.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

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

@ExtendWith(MockitoExtension.class)
class FareServiceTest {

    @Mock
    private FareRepository fareRepository;

    private FareService service;

    @BeforeEach
    void setUp() {
        FareProperties properties = new FareProperties();
        service = new FareService(fareRepository, new FareCalculationService(properties),
                new DistanceCalculator(), properties, new LocationResolver());
    }

    @Test
    void estimateByPlaceNamesDoesNotPersistAnything() {
        FareEstimateResponse response = service.estimate(
                new FareEstimateRequest("Colombo 03", "Colombo 07"));

        assertThat(response.pickup()).isEqualTo("Colombo 03");
        assertThat(response.destination()).isEqualTo("Colombo 07");
        assertThat(response.distanceKm()).isPositive();
        assertThat(response.estimatedFare()).isGreaterThanOrEqualTo(new BigDecimal("250.00"));
        assertThat(response.currency()).isEqualTo("LKR");
        verify(fareRepository, never()).save(any());
    }

    @Test
    void estimateWithUnknownPlaceIsRejected() {
        assertThatThrownBy(() -> service.estimate(new FareEstimateRequest("Atlantis", "Galle")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Atlantis");
    }

    @Test
    void finalFareIsStoredWithBreakdown() {
        when(fareRepository.existsByRideId("RIDE001")).thenReturn(false);
        when(fareRepository.save(any(Fare.class))).thenAnswer(inv -> inv.getArgument(0));

        FareResponse response = service.calculateFinalFare(
                new FinalFareRequest("RIDE001", "PASS001", new BigDecimal("2.77"), null, null));

        assertThat(response.rideId()).isEqualTo("RIDE001");
        assertThat(response.passengerId()).isEqualTo("PASS001");
        assertThat(response.distanceKm()).isEqualByComparingTo("2.77");
        assertThat(response.baseFare()).isEqualByComparingTo("150.00");
        assertThat(response.ratePerKm()).isEqualByComparingTo("80.00");
        assertThat(response.totalFare()).isEqualByComparingTo("371.60");
        assertThat(response.calculatedAt()).isNotNull();
    }

    @Test
    void finalFareFromPickupAndDestination() {
        when(fareRepository.existsByRideId("RIDE002")).thenReturn(false);
        when(fareRepository.save(any(Fare.class))).thenAnswer(inv -> inv.getArgument(0));

        FareResponse response = service.calculateFinalFare(
                new FinalFareRequest("RIDE002", "PASS001", null, "Colombo 03", "Kandy Central"));

        // Colombo 03 to Kandy is roughly 95 km in a straight line
        assertThat(response.distanceKm()).isBetween(new BigDecimal("85"), new BigDecimal("105"));
        assertThat(response.totalFare()).isGreaterThan(new BigDecimal("6000"));
        assertThat(response.currency()).isEqualTo("LKR");
    }

    @Test
    void finalFareWithoutDistanceOrLocationsIsRejected() {
        assertThatThrownBy(() -> service.calculateFinalFare(
                new FinalFareRequest("RIDE001", "PASS001", null, null, null)))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.calculateFinalFare(
                new FinalFareRequest("RIDE001", "PASS001", null, "Colombo 03", " ")))
                .isInstanceOf(InvalidRequestException.class);
        verify(fareRepository, never()).save(any());
    }

    @Test
    void duplicateFinalFareIsRejected() {
        when(fareRepository.existsByRideId("RIDE001")).thenReturn(true);

        assertThatThrownBy(() -> service.calculateFinalFare(
                new FinalFareRequest("RIDE001", "PASS001", BigDecimal.ONE, null, null)))
                .isInstanceOf(ConflictException.class);
        verify(fareRepository, never()).save(any());
    }

    @Test
    void concurrentDuplicateIsMappedToConflict() {
        when(fareRepository.existsByRideId("RIDE001")).thenReturn(false);
        when(fareRepository.save(any(Fare.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThatThrownBy(() -> service.calculateFinalFare(
                new FinalFareRequest("RIDE001", "PASS001", BigDecimal.ONE, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void missingFareThrowsNotFound() {
        when(fareRepository.findByRideId("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFareByRideId("NOPE"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
