package com.jigmnnaga.application.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record ChatMessagePageResponse(
        List<ChatMessageResponse> messages,
        Long nextCursor,
        boolean hasNext
) {
}
