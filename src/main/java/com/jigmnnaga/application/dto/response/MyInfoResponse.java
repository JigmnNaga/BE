package com.jigmnnaga.application.dto.response;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record MyInfoResponse(
        Long memberId,
        String nickname,
        String profileImageUrl,
        String email,
        DefaultLocationResponse defaultLocation
) {

    @Builder
    public record DefaultLocationResponse(
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }
}
