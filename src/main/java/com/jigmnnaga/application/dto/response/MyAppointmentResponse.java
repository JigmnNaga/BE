package com.jigmnnaga.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;

@Builder
public record MyAppointmentResponse(
        Long appointmentId,
        String title,
        boolean confirmed,
        LocalDate confirmedDate,
        @JsonFormat(pattern = "HH:mm") LocalTime confirmedTime,
        String confirmedLocationName,
        String detailPlaceName,
        LocalDate candidateStartDate,
        LocalDate candidateEndDate,
        int memberCount
) {
}
