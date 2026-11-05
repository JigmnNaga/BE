package com.jigmnnaga.application.dto.response;

import com.jigmnnaga.domain.entity.NotificationType;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String body,
        Long appointmentId,
        boolean read,
        LocalDateTime createdAt
) {
}
