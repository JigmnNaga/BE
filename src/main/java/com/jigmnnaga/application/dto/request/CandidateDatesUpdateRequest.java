package com.jigmnnaga.application.dto.request;

import com.jigmnnaga.domain.entity.DateSelectionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CandidateDatesUpdateRequest(
        @Schema(description = "날짜 선택 방식 (SINGLE: 하루, RANGE: 기간, MULTIPLE: 날짜별)", example = "RANGE")
        @NotNull DateSelectionType dateSelectionType,

        @Schema(description = "후보 날짜 목록", example = "[\"2026-09-04\", \"2026-09-05\"]")
        @NotEmpty List<@NotNull LocalDate> candidateDates
) {
}
