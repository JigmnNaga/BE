package com.jigmnnaga.application.dto.response;

import lombok.Builder;

@Builder
public record LoginResponse(
        Long memberId,
        String accessToken,
        String refreshToken,
        boolean isNewMember
) {
}
