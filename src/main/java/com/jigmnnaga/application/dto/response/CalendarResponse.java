package com.jigmnnaga.application.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record CalendarResponse(
        List<CalendarDayResponse> days
) {

    @Builder
    public record CalendarDayResponse(
            LocalDate date,
            List<MyAppointmentResponse> appointments
    ) {
    }
}
