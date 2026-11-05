package com.jigmnnaga.application.dto.response;

import com.jigmnnaga.domain.entity.NotificationType;
import lombok.Builder;

@Builder
public record ReminderSendResponse(
        NotificationType type,
        int sentCount
) {
}
