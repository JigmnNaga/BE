package com.jigmnnaga.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jigmnnaga.domain.entity.ProfileColor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.Builder;

@Builder
public record MyAppointmentResponse(
        Long appointmentId,
        String title,
        boolean canceled,
        boolean confirmed,
        LocalDate confirmedDate,
        @JsonFormat(pattern = "HH:mm") LocalTime confirmedTime,
        String confirmedLocationName,
        String detailPlaceName,
        LocalDate candidateStartDate,
        LocalDate candidateEndDate,
        int memberCount,
        List<MemberProfileResponse> participants,
        int unreadMessageCount
) {

    @Builder
    public record MemberProfileResponse(
            Long memberId,
            String nickname,
            String profileImageUrl,
            ProfileColor profileColor
    ) {
    }
}
