package com.ridelink.driver.dto.request;

import com.ridelink.driver.enums.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAvailabilityRequest {

    @NotNull(message = "Availability status is required")
    private AvailabilityStatus availabilityStatus;
}