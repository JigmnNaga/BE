package com.jigmnnaga.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChatReadRequest(
        @Schema(description = "마지막으로 읽은 메시지 ID", example = "123")
        @NotNull @Positive Long lastReadMessageId
) {
}
