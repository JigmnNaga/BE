package com.jigmnnaga.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record LocationSharingUpdateRequest(

        @Schema(description = "위치 공유 여부 (false: 종료, true: 다시 켜기)", example = "false")
        @NotNull
        Boolean enabled
) {
}
