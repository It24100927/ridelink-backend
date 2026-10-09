package com.ridelink.driver_vehicle_service.dto.response;

import com.ridelink.driver_vehicle_service.model.Driver;
import com.ridelink.driver_vehicle_service.model.Driver.Availability;
import lombok.Builder;
import lombok.Data;

/**
 * Response element for GET /api/drivers/eligible?serviceArea=X.
 * Contract shape (per-element):
 *   { "driverId", "accountId", "serviceArea", "currentLatitude",
 *     "currentLongitude", "availability" }
 *
 * Note: "accountId" is used here (not "userId") — the contract doc example was
 * inconsistent and accountId is the correct field throughout the rest of the spec.
 * No timestamps are included in this response per the contract.
 */
@Data
@Builder
public class EligibleDriverResponse {

    private String driverId;
    private String accountId;
    private String serviceArea;
    private Double currentLatitude;
    private Double currentLongitude;
    private Availability availability;

    public static EligibleDriverResponse from(Driver driver) {
        return EligibleDriverResponse.builder()
                .driverId(driver.getDriverId())
                .accountId(driver.getAccountId())
                .serviceArea(driver.getServiceArea())
                .currentLatitude(driver.getCurrentLatitude())
                .currentLongitude(driver.getCurrentLongitude())
                .availability(driver.getAvailability())
                .build();
    }
}
