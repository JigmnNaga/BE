package com.jigmnnaga.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record NotificationSettingRequest(
        @Schema(description = "약속방 알림 받기 여부", example = "false")
        @NotNull Boolean enabled
) {
}
