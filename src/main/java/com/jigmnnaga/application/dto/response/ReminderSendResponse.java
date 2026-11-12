package com.jigmnnaga.application.dto.response;

import com.jigmnnaga.domain.entity.NotificationType;
import java.util.List;
import lombok.Builder;

@Builder
public record ReminderSendResponse(
        int sentCount,
        List<ReminderResultResponse> reminders
) {

    @Builder
    public record ReminderResultResponse(
            NotificationType type,
            int sentCount
    ) {
    }
}
