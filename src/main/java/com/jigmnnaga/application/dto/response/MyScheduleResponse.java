package com.jigmnnaga.application.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record MyScheduleResponse(
        boolean submitted,
        List<AvailabilityResponse> availabilities,
        Long preferredLocationId
) {
}
