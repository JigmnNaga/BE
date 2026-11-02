package com.jigmnnaga.application.dto.response;

import com.jigmnnaga.domain.entity.DateSelectionType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record AppointmentCreateResponse(
        Long appointmentId,
        String title,
        DateSelectionType dateSelectionType,
        List<LocalDate> candidateDates,
        List<String> candidateLocationNames,
        LocalDateTime responseDeadline,
        String inviteCode,
        String inviteUrl
) {
}
