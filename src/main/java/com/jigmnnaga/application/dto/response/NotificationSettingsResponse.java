package com.jigmnnaga.application.dto.response;

import lombok.Builder;

@Builder
public record NotificationSettingsResponse(
        boolean allNotificationsEnabled,
        boolean appointmentReminderEnabled,
        boolean departureTimeEnabled,
        boolean friendArrivalEnabled,
        boolean marketingEnabled
) {
}
