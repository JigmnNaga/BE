package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.response.ChatMessagePageResponse;
import com.jigmnnaga.application.dto.response.ChatMessageResponse;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.ChatMessage;
import com.jigmnnaga.domain.entity.ChatMessageType;
import com.jigmnnaga.domain.entity.Member;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageMapper {

    public ChatMessage toTextMessage(Appointment appointment, Member sender, String content) {
        return ChatMessage.builder()
                .appointment(appointment)
                .sender(sender)
                .type(ChatMessageType.TEXT)
                .content(content)
                .build();
    }

    public ChatMessage toSystemMessage(Appointment appointment, String content) {
        return ChatMessage.builder()
                .appointment(appointment)
                .type(ChatMessageType.SYSTEM)
                .content(content)
                .build();
    }

    public ChatMessageResponse toResponse(ChatMessage chatMessage) {
        Member sender = chatMessage.getSender();
        return ChatMessageResponse.builder()
                .messageId(chatMessage.getId())
                .appointmentId(chatMessage.getAppointment().getId())
                .type(chatMessage.getType())
                .senderId(sender == null ? null : sender.getId())
                .senderNickname(sender == null ? null : sender.getNickname())
                .senderProfileImageUrl(sender == null ? null : sender.getProfileImageUrl())
                .content(chatMessage.getContent())
                .sentAt(chatMessage.getCreatedAt())
                .build();
    }

    public ChatMessagePageResponse toPageResponse(List<ChatMessage> newestFirst, int size) {
        boolean hasNext = newestFirst.size() > size;
        List<ChatMessage> page = newestFirst.subList(0, Math.min(size, newestFirst.size()));

        return ChatMessagePageResponse.builder()
                .messages(page.stream()
                        .sorted(Comparator.comparing(ChatMessage::getId))
                        .map(this::toResponse)
                        .toList())
                .nextCursor(hasNext ? page.getLast().getId() : null)
                .hasNext(hasNext)
                .build();
    }
}
