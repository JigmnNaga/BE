package com.jigmnnaga.application.dto.response;

import com.jigmnnaga.domain.entity.DateSelectionType;
import com.jigmnnaga.domain.entity.ParticipantRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record AppointmentDetailResponse(
        Long appointmentId,
        String title,
        DateSelectionType dateSelectionType,
        List<LocalDate> candidateDates,
        List<CandidateLocationResponse> candidateLocations,
        LocalDateTime responseDeadline,
        String inviteUrl,
        ParticipantRole myRole,
        int joinedCount,
        int scheduleSubmittedCount,
        List<MemberStatusResponse> members
) {

    @Builder
    public record CandidateLocationResponse(
            Long locationId,
            String placeName,
            String address,
            Double latitude,
            Double longitude
    ) {
    }

    @Builder
    public record MemberStatusResponse(
            Long memberId,
            String nickname,
            String profileImageUrl,
            ParticipantRole role,
            boolean scheduleSubmitted
    ) {
    }
}
