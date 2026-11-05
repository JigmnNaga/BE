package com.jigmnnaga.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record HostDelegateRequest(
        @Schema(description = "새 방장 회원 ID", example = "2")
        @NotNull Long memberId
) {
}
