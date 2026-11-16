package com.jigmnnaga.application.dto.response;

import lombok.Builder;

@Builder
public record AppointmentStatsResponse(
        int totalCount,
        int onTimeCount,
        int lateCount,
        Double onTimeRate
) {
}
