package com.jigmnnaga.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record ConfirmDateTimeRequest(
        @Schema(description = "확정 날짜 (후보 날짜 중 하나)", example = "2026-09-06")
        @NotNull LocalDate date,

        @Schema(description = "확정 시간", type = "string", example = "17:00")
        @NotNull LocalTime time
) {
}
