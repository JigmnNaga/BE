package com.jigmnnaga.application.dto.response;

import com.jigmnnaga.domain.entity.AppointmentParticipant;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record DepartureLocationResponse(

        @Schema(description = "출발 위치 이름", example = "강남역")
        String name,

        @Schema(description = "출발 위치 위도", example = "37.4979420")
        BigDecimal latitude,

        @Schema(description = "출발 위치 경도", example = "127.0276210")
        BigDecimal longitude
) {
    public static DepartureLocationResponse from(AppointmentParticipant participant) {
        return new DepartureLocationResponse(
                participant.getDepartureLocationName(),
                participant.getDepartureLatitude(),
                participant.getDepartureLongitude()
        );
    }
}