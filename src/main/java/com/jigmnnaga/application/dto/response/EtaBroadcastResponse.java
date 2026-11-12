package com.jigmnnaga.application.dto.response;

import java.time.LocalDateTime;

public record EtaBroadcastResponse(
        Long memberId,
        LocalDateTime estimatedArrivalTime,
        Integer remainingMinutes,
        LocalDateTime calculatedAt
) {
}
