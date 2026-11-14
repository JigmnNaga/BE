package com.jigmnnaga.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ScheduleSubmitRequest(
        @Schema(description = "날짜별 가능 시간 목록 (생략하면 기존 값 유지, 빈 목록이면 모두 해제)")
        List<@Valid @NotNull AvailabilityRequest> availabilities,

        @Schema(description = "희망 장소 ID (약속의 후보 장소 중 하나, 생략하면 기존 값 유지)", example = "1")
        Long preferredLocationId
) {

    public record AvailabilityRequest(
            @Schema(description = "가능한 날짜", example = "2026-09-05")
            @NotNull LocalDate date,

            @Schema(description = "시작 시간", type = "string", example = "12:00")
            @NotNull LocalTime startTime,

            @Schema(description = "종료 시간", type = "string", example = "22:00")
            @NotNull LocalTime endTime
    ) {
    }
}
