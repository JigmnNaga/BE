package com.jigmnnaga.application.dto.response;

import com.jigmnnaga.domain.entity.ProfileColor;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record MyInfoResponse(
        Long memberId,
        String nickname,
        String profileImageUrl,
        ProfileColor profileColor,
        String email,
        LocalDate joinedAt,
        List<MemberLocationResponse> locations
) {
}
