package com.jigmnnaga.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record NotificationSettingsRequest(

        @Schema(description = "전체 알림 사용 여부", example = "true")
        @NotNull
        Boolean allNotificationsEnabled,

        @Schema(description = "약속 알림 사용 여부", example = "true")
        @NotNull
        Boolean appointmentReminderEnabled,

        @Schema(description = "출발 시간 알림 사용 여부", example = "true")
        @NotNull
        Boolean departureTimeEnabled,

        @Schema(description = "친구 도착 알림 사용 여부", example = "true")
        @NotNull
        Boolean friendArrivalEnabled,

        @Schema(description = "마케팅 알림 사용 여부", example = "false")
        @NotNull
        Boolean marketingEnabled
) {
}
