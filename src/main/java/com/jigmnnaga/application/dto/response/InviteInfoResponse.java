package com.jigmnnaga.application.dto.response;

import com.jigmnnaga.domain.entity.ParticipationStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record InviteInfoResponse(
        Long appointmentId,
        String title,
        String hostNickname,
        LocalDateTime responseDeadline,
        boolean expired,
        int respondedCount,
        int joinedCount,
        ParticipationStatus myStatus,
        List<ParticipantResponse> participants
) {
}
