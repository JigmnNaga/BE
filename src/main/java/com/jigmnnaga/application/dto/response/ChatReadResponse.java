package com.jigmnnaga.application.dto.response;

import lombok.Builder;

@Builder
public record ChatReadResponse(
        Long memberId,
        Long previousLastReadMessageId,
        Long lastReadMessageId
) {
}
