package com.jigmnnaga.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppointmentTitleUpdateRequest(
        @Schema(description = "약속 이름", example = "중간고사 카공 모임")
        @NotBlank @Size(max = 30) String title
) {
}
