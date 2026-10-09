package com.ridelink.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.ridelink.payment.exception.InvalidRequestException;
import com.ridelink.payment.service.LocationResolver.Coordinates;

class LocationResolverTest {

    private final LocationResolver resolver = new LocationResolver();

    @Test
    void knownPlaceIsResolved() {
        Coordinates c = resolver.resolve("Colombo 03");
        assertThat(c.latitude()).isBetween(6.8, 7.0);
        assertThat(c.longitude()).isBetween(79.8, 79.9);
    }

    @Test
    void placeNameIgnoresCaseAndExtraSpaces() {
        assertThat(resolver.resolve("  kandy   CENTRAL ")).isEqualTo(resolver.resolve("Kandy Central"));
    }

    @Test
    void coordinatesStringIsParsed() {
        assertThat(resolver.resolve("6.9271,79.8612")).isEqualTo(new Coordinates(6.9271, 79.8612));
        assertThat(resolver.resolve(" 6.9271 , 79.8612 ")).isEqualTo(new Coordinates(6.9271, 79.8612));
    }

    @Test
    void outOfRangeCoordinatesAreRejected() {
        assertThatThrownBy(() -> resolver.resolve("91,79.8"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("91,79.8");
        assertThatThrownBy(() -> resolver.resolve("6.9,181"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void unknownPlaceIsRejectedWithItsName() {
        assertThatThrownBy(() -> resolver.resolve("Atlantis"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Atlantis");
    }

    @Test
    void malformedCoordinatesAreRejected() {
        assertThatThrownBy(() -> resolver.resolve("abc,def"))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> resolver.resolve("1,2,3"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void requiredPlacesExist() {
        for (String place : new String[] {"Colombo 03", "Colombo 01", "Colombo 07", "Kandy Central", "Kandy",
                "Galle", "Negombo", "Mount Lavinia", "Dehiwala", "Nugegoda", "Maharagama", "Kaduwela", "Malabe",
                "Gampaha", "Jaffna", "Matara", "Anuradhapura", "Kurunegala", "Ratnapura", "Nuwara Eliya"}) {
            assertThat(resolver.resolve(place)).as(place).isNotNull();
        }
    }
}
