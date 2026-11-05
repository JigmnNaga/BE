package com.jigmnnaga.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ReceivedInviteResponse(
        Long appointmentId,
        String title,
        String hostNickname,
        String inviteCode,
        LocalDate candidateStartDate,
        LocalDate candidateEndDate,
        LocalDateTime responseDeadline
) {
}
