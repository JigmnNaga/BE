package com.jigmnnaga.application.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record MyInfoResponse(
        Long memberId,
        String nickname,
        String profileImageUrl,
        String email,
        List<MemberLocationResponse> locations
) {
}
