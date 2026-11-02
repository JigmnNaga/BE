package com.jigmnnaga.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.Builder;

@Builder
public record MyScheduleResponse(
        boolean submitted,
        List<AvailabilityResponse> availabilities,
        Long preferredLocationId
) {

    @Builder
    public record AvailabilityResponse(
            LocalDate date,
            @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @JsonFormat(pattern = "HH:mm") LocalTime endTime
    ) {
    }
}
