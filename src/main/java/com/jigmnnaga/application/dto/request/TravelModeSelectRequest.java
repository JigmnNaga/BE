package com.jigmnnaga.application.dto.request;

import com.jigmnnaga.domain.entity.TravelMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TravelModeSelectRequest(
        @Schema(description = "이동수단 (BUS, SUBWAY, TAXI)", example = "BUS")
        @NotNull TravelMode mode,

        @Schema(description = "선택한 이동수단의 예상 소요 시간(분) — 이동수단 후보 조회 응답의 durationMinutes", example = "20")
        @NotNull @Min(1) @Max(600) Integer durationMinutes
) {
}
