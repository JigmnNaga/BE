package com.jigmnnaga.application.event;

import com.jigmnnaga.application.dto.response.ChatMessageResponse;

public record ChatMessageCreatedEvent(
        ChatMessageResponse message
) {
}
