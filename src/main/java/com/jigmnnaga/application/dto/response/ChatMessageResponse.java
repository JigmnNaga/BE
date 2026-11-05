package com.jigmnnaga.application.dto.response;

import com.jigmnnaga.domain.entity.ChatMessageType;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ChatMessageResponse(
        Long messageId,
        Long appointmentId,
        ChatMessageType type,
        Long senderId,
        String senderNickname,
        String senderProfileImageUrl,
        String content,
        LocalDateTime sentAt
) {
}
