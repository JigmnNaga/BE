package com.jigmnnaga.application.dto.response;

import com.jigmnnaga.domain.entity.DepartureStatus;
import java.time.LocalDateTime;

public record LocationBroadcastResponse(
        Long memberId,
        double latitude,
        double longitude,
        DepartureStatus departureStatus,
        LocalDateTime updatedAt
) {
}
