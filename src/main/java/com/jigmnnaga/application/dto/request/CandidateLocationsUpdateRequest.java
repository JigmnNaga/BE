package com.jigmnnaga.application.dto.request;

import com.jigmnnaga.application.dto.request.AppointmentCreateRequest.CandidateLocationRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CandidateLocationsUpdateRequest(
        @Schema(description = "유지할 기존 후보 장소 ID 목록 (빠진 장소는 삭제)", example = "[1, 2]")
        @NotNull List<@NotNull Long> keepLocationIds,

        @Schema(description = "새로 추가할 후보 장소 목록")
        @NotNull List<@Valid @NotNull CandidateLocationRequest> newLocations
) {
}
