package com.jigmnnaga.application.dto.response;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record MemberLocationResponse(
        Long locationId,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
