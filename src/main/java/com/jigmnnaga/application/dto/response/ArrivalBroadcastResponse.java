package com.jigmnnaga.application.dto.response;

import java.time.LocalDateTime;

public record ArrivalBroadcastResponse(
        Long memberId,
        LocalDateTime arrivedAt
) {
}
