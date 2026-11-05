package com.jigmnnaga.application.event;

import java.util.List;
import java.util.Map;

public record PushEvent(
        List<Long> recipientIds,
        Long appointmentId,
        String title,
        String body,
        Map<String, String> data
) {
}
