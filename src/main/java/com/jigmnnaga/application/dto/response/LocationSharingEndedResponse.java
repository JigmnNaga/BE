package com.jigmnnaga.application.dto.response;

import java.time.LocalDateTime;

public record LocationSharingEndedResponse(
        Long memberId,
        Reason reason,
        LocalDateTime endedAt
) {

    public enum Reason {
        ARRIVED,
        USER_REQUEST
    }
}
