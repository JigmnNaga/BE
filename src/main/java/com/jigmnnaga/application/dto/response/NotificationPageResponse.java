package com.jigmnnaga.application.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record NotificationPageResponse(
        List<NotificationResponse> notifications,
        Long nextCursor,
        boolean hasNext
) {
}
