package com.jigmnnaga.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jigmnnaga.domain.entity.AttendanceStatus;
import com.jigmnnaga.domain.entity.DateSelectionType;
import com.jigmnnaga.domain.entity.ParticipantRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.Builder;

@Builder
public record AppointmentDetailResponse(
        Long appointmentId,
        String title,
        boolean canceled,
        DateSelectionType dateSelectionType,
        List<LocalDate> candidateDates,
        List<CandidateLocationResponse> candidateLocations,
        LocalDateTime responseDeadline,
        String inviteUrl,
        boolean confirmed,
        LocalDate confirmedDate,
        @JsonFormat(pattern = "HH:mm") LocalTime confirmedTime,
        CandidateLocationResponse confirmedLocation,
        DetailPlaceResponse detailPlace,
        ParticipantRole myRole,
        AttendanceStatus myAttendanceStatus,
        int joinedCount,
        int scheduleSubmittedCount,
        List<MemberStatusResponse> members
) {

    @Builder
    public record CandidateLocationResponse(
            Long locationId,
            String placeName,
            String address,
            String lineNames,
            Double latitude,
            Double longitude
    ) {
    }

    @Builder
    public record DetailPlaceResponse(
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
            boolean scheduleSubmitted,
            AttendanceStatus attendanceStatus
    ) {
    }
}
