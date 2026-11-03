package com.jigmnnaga.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ResponseDeadlineUpdateRequest(
        @Schema(description = "참여 응답 기한", example = "2026-09-03T23:59:59")
        @NotNull LocalDateTime responseDeadline
) {
}
