package com.jigmnnaga.application.dto.request;

import com.jigmnnaga.domain.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AttendanceRespondRequest(
        @Schema(description = "참석 응답 (ATTENDING: 참석할게요, NOT_ATTENDING: 참석이 어려워요)", example = "ATTENDING")
        @NotNull AttendanceStatus attendanceStatus
) {
}
