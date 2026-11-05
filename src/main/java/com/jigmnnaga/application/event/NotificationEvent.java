package com.jigmnnaga.application.event;

import com.jigmnnaga.domain.entity.NotificationType;
import java.util.List;

public record NotificationEvent(
        List<Long> recipientIds,
        Long appointmentId,
        NotificationType type,
        String title,
        String body
) {
}
