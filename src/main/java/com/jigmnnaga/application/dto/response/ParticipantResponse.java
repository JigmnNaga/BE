package com.jigmnnaga.application.dto.response;

import com.jigmnnaga.domain.entity.ParticipantRole;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import com.jigmnnaga.domain.entity.ProfileColor;
import lombok.Builder;

@Builder
public record ParticipantResponse(
        Long memberId,
        String nickname,
        String profileImageUrl,
        ProfileColor profileColor,
        ParticipantRole role,
        ParticipationStatus status
) {
}
