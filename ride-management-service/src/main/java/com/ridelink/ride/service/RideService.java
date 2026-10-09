package com.ridelink.ride.service;

import com.ridelink.ride.client.DriverClient;
import com.ridelink.ride.client.FareClient;
import com.ridelink.ride.dto.AcceptRideRequest;
import com.ridelink.ride.dto.AssignDriverRequest;
import com.ridelink.ride.dto.AvailableDriverResponse;
import com.ridelink.ride.dto.CancelRideRequest;
import com.ridelink.ride.dto.CompleteRideRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.FareEstimateRequest;
import com.ridelink.ride.dto.FareEstimateResponse;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.dto.StartRideRequest;
import com.ridelink.ride.exception.DriverNotAssignedException;
import com.ridelink.ride.exception.InvalidRideStateException;
import com.ridelink.ride.exception.NoAvailableDriverException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final DriverClient driverClient;
    private final FareClient fareClient;

    public RideService(
            RideRepository rideRepository,
            DriverClient driverClient,
            FareClient fareClient) {

        this.rideRepository = rideRepository;
        this.driverClient = driverClient;
        this.fareClient = fareClient;
    }

    public RideResponse createRide(CreateRideRequest request) {

        Ride ride = new Ride(
                request.getPassengerId(),
                request.getPickupLocation(),
                request.getDestination()
        );

        ride.setStatus(RideStatus.REQUESTED);

        /*
         * Fare estimation is requested from the Fare & Payment Service.
         */
        FareEstimateRequest fareRequest =
                new FareEstimateRequest(
                        request.getPickupLocation(),
                        request.getDestination()
                );

        FareEstimateResponse fareResponse =
                fareClient.estimateFare(fareRequest);

        ride.setEstimatedFare(
                fareResponse.getEstimatedFare()
        );

        /*
         * Ask Driver & Vehicle Service for eligible drivers.
         */
        List<AvailableDriverResponse> drivers =
                driverClient.getEligibleDrivers();

        if (drivers.isEmpty()) {

            /*
             * Keep the ride as REQUESTED in our database.
             */
            Ride savedRide = rideRepository.save(ride);

            throw new NoAvailableDriverException(
                    "No available driver found for this ride"
            );
        }

        /*
         * Simple documented selection approach:
         * select the first driver returned by the Driver Service.
         */
        AvailableDriverResponse selectedDriver =
                drivers.get(0);

        ride.setDriverId(selectedDriver.getDriverId());
        ride.setStatus(RideStatus.ASSIGNED);
        ride.setUpdatedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);

        return new RideResponse(savedRide);
    }

    public RideResponse assignDriver(
            String rideId,
            String driverId) {

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() ->
                        new RideNotFoundException(
                                "Ride not found: " + rideId
                        )
                );

        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new InvalidRideStateException(
                    "Driver can only be assigned to a requested ride"
            );
        }

        List<AvailableDriverResponse> drivers =
                driverClient.getEligibleDrivers();

        boolean driverAvailable = drivers.stream()
                .anyMatch(driver ->
                        driverId.equals(driver.getDriverId())
                );

        if (!driverAvailable) {
            throw new NoAvailableDriverException(
                    "The requested driver is not currently eligible"
            );
        }

        ride.setDriverId(driverId);
        ride.setStatus(RideStatus.ASSIGNED);
        ride.setUpdatedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);

        return new RideResponse(savedRide);
    }

    public RideResponse acceptRide(
            String rideId,
            AcceptRideRequest request) {

        Ride ride = findRide(rideId);

        if (ride.getStatus() != RideStatus.ASSIGNED) {
            throw new InvalidRideStateException(
                    "Only an assigned ride can be accepted"
            );
        }

        validateAssignedDriver(
                ride,
                request.getDriverId()
        );

        ride.setStatus(RideStatus.ACCEPTED);
        ride.setUpdatedAt(LocalDateTime.now());

        return new RideResponse(
                rideRepository.save(ride)
        );
    }

    public RideResponse startRide(
            String rideId,
            StartRideRequest request) {

        Ride ride = findRide(rideId);

        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new InvalidRideStateException(
                    "Only an accepted ride can be started"
            );
        }

        validateAssignedDriver(
                ride,
                request.getDriverId()
        );

        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setUpdatedAt(LocalDateTime.now());

        return new RideResponse(
                rideRepository.save(ride)
        );
    }

    public RideResponse completeRide(
            String rideId,
            CompleteRideRequest request) {

        Ride ride = findRide(rideId);

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new InvalidRideStateException(
                    "Only an in-progress ride can be completed"
            );
        }

        validateAssignedDriver(
                ride,
                request.getDriverId()
        );

        ride.setStatus(RideStatus.COMPLETED);
        ride.setUpdatedAt(LocalDateTime.now());

        return new RideResponse(
                rideRepository.save(ride)
        );
    }

    public RideResponse cancelRide(
            String rideId,
            CancelRideRequest request) {

        Ride ride = findRide(rideId);

        if (!request.getPassengerId()
                .equals(ride.getPassengerId())) {

            throw new DriverNotAssignedException(
                    "Only the passenger who created the ride can cancel it"
            );
        }

        if (ride.getStatus() == RideStatus.COMPLETED
                || ride.getStatus() == RideStatus.CANCELLED
                || ride.getStatus() == RideStatus.IN_PROGRESS) {

            throw new InvalidRideStateException(
                    "Ride cannot be cancelled in its current state"
            );
        }

        ride.setStatus(RideStatus.CANCELLED);
        ride.setUpdatedAt(LocalDateTime.now());

        return new RideResponse(
                rideRepository.save(ride)
        );
    }

    public RideResponse getRideById(String rideId) {

        return new RideResponse(findRide(rideId));
    }

    public List<RideResponse> getRidesByPassenger(
            String passengerId) {

        return rideRepository.findByPassengerId(passengerId)
                .stream()
                .map(RideResponse::new)
                .toList();
    }

    public List<RideResponse> getRidesByDriver(
            String driverId) {

        return rideRepository.findByDriverId(driverId)
                .stream()
                .map(RideResponse::new)
                .toList();
    }

    private Ride findRide(String rideId) {

        return rideRepository.findById(rideId)
                .orElseThrow(() ->
                        new RideNotFoundException(
                                "Ride not found: " + rideId
                        )
                );
    }

    private void validateAssignedDriver(
            Ride ride,
            String driverId) {

        if (!driverId.equals(ride.getDriverId())) {

            throw new DriverNotAssignedException(
                    "This driver is not assigned to the ride"
            );
        }
    }
}