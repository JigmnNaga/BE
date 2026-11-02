package com.jigmnnaga.application.dto.response;

import com.jigmnnaga.domain.entity.ParticipantRole;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import lombok.Builder;

@Builder
public record ParticipantResponse(
        Long memberId,
        String nickname,
        String profileImageUrl,
        ParticipantRole role,
        ParticipationStatus status
) {
}
