package com.ridelink.payment.service;

import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ridelink.payment.exception.InvalidRequestException;

/**
 * Converts a location string into latitude/longitude.
 *
 * Accepted inputs:
 *  - a known Sri Lankan place name from the built-in gazetteer (case-insensitive,
 *    surrounding and repeated whitespace ignored), for example "Colombo 03"
 *  - simulated coordinates written as "lat,lon", for example "6.9271,79.8612"
 */
@Service
public class LocationResolver {

    /** A resolved point on the map. */
    public record Coordinates(double latitude, double longitude) {
    }

    private static final Map<String, Coordinates> GAZETTEER = Map.ofEntries(
            Map.entry("colombo 01", new Coordinates(6.9344, 79.8428)),
            Map.entry("colombo 03", new Coordinates(6.9147, 79.8523)),
            Map.entry("colombo 07", new Coordinates(6.9023, 79.8616)),
            Map.entry("kandy central", new Coordinates(7.2906, 80.6337)),
            Map.entry("kandy", new Coordinates(7.2906, 80.6337)),
            Map.entry("galle", new Coordinates(6.0535, 80.2210)),
            Map.entry("negombo", new Coordinates(7.2083, 79.8358)),
            Map.entry("mount lavinia", new Coordinates(6.8389, 79.8653)),
            Map.entry("dehiwala", new Coordinates(6.8518, 79.8650)),
            Map.entry("nugegoda", new Coordinates(6.8649, 79.8997)),
            Map.entry("maharagama", new Coordinates(6.8480, 79.9265)),
            Map.entry("kaduwela", new Coordinates(6.9333, 79.9833)),
            Map.entry("malabe", new Coordinates(6.9061, 79.9697)),
            Map.entry("gampaha", new Coordinates(7.0873, 79.9925)),
            Map.entry("jaffna", new Coordinates(9.6615, 80.0255)),
            Map.entry("matara", new Coordinates(5.9549, 80.5550)),
            Map.entry("anuradhapura", new Coordinates(8.3114, 80.4037)),
            Map.entry("kurunegala", new Coordinates(7.4863, 80.3647)),
            Map.entry("ratnapura", new Coordinates(6.6828, 80.3992)),
            Map.entry("nuwara eliya", new Coordinates(6.9497, 80.7891)),
            Map.entry("batticaloa", new Coordinates(7.7310, 81.6747)),
            Map.entry("trincomalee", new Coordinates(8.5874, 81.2152)));

    /**
     * @throws InvalidRequestException if the text is neither a known place nor valid coordinates
     */
    public Coordinates resolve(String location) {
        if (location == null || location.isBlank()) {
            throw new InvalidRequestException("Location must not be blank");
        }

        String normalised = location.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);

        Coordinates known = GAZETTEER.get(normalised);
        if (known != null) {
            return known;
        }

        if (normalised.contains(",")) {
            return parseCoordinates(location, normalised);
        }

        throw new InvalidRequestException("Unknown location: '" + location.trim() + "'");
    }

    private Coordinates parseCoordinates(String original, String normalised) {
        String[] parts = normalised.split(",", -1);
        if (parts.length != 2) {
            throw new InvalidRequestException("Unknown location: '" + original.trim() + "'");
        }

        double latitude;
        double longitude;
        try {
            latitude = Double.parseDouble(parts[0].trim());
            longitude = Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException ex) {
            throw new InvalidRequestException("Unknown location: '" + original.trim() + "'");
        }

        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new InvalidRequestException("Coordinates out of range (latitude -90..90, longitude -180..180): '"
                    + original.trim() + "'");
        }
        return new Coordinates(latitude, longitude);
    }
}
