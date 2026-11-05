package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.response.ChatMessagePageResponse;
import com.jigmnnaga.application.dto.response.ChatMessageResponse;
import com.jigmnnaga.application.dto.response.ChatReadResponse;
import com.jigmnnaga.application.event.PushEvent;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.ChatMessage;
import com.jigmnnaga.domain.entity.ChatMessageType;
import com.jigmnnaga.domain.entity.Member;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageMapper {

    private static final int MAX_PUSH_BODY_LENGTH = 100;
    private static final String CHAT_PUSH_TYPE = "CHAT_MESSAGE";

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

    public ChatMessageResponse toResponse(ChatMessage chatMessage, Integer unreadCount) {
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
                .unreadCount(chatMessage.getType() == ChatMessageType.SYSTEM ? null : unreadCount)
                .build();
    }

    public ChatMessagePageResponse toPageResponse(List<ChatMessage> newestFirst, int size,
                                                  List<AppointmentParticipant> participants,
                                                  AppointmentParticipant me) {
        boolean hasNext = newestFirst.size() > size;
        List<ChatMessage> page = newestFirst.subList(0, Math.min(size, newestFirst.size()));

        return ChatMessagePageResponse.builder()
                .messages(page.stream()
                        .sorted(Comparator.comparing(ChatMessage::getId))
                        .map(chatMessage -> toResponse(chatMessage, countUnreadMembers(chatMessage, participants)))
                        .toList())
                .myLastReadMessageId(me.getLastReadMessageId())
                .nextCursor(hasNext ? page.getLast().getId() : null)
                .hasNext(hasNext)
                .build();
    }

    public ChatReadResponse toReadResponse(Long memberId, Long previousLastReadMessageId, Long lastReadMessageId) {
        return ChatReadResponse.builder()
                .memberId(memberId)
                .previousLastReadMessageId(previousLastReadMessageId)
                .lastReadMessageId(lastReadMessageId)
                .build();
    }

    private int countUnreadMembers(ChatMessage chatMessage, List<AppointmentParticipant> participants) {
        Member sender = chatMessage.getSender();
        return (int) participants.stream()
                .filter(participant -> sender == null || !participant.getMember().getId().equals(sender.getId()))
                .filter(participant -> !participant.hasRead(chatMessage.getId()))
                .count();
    }

    public PushEvent toPushEvent(ChatMessage chatMessage, List<Long> recipientIds) {
        String content = chatMessage.getContent();
        if (content.length() > MAX_PUSH_BODY_LENGTH) {
            content = content.substring(0, MAX_PUSH_BODY_LENGTH) + "…";
        }
        return new PushEvent(recipientIds, chatMessage.getAppointment().getId(), chatMessage.getAppointment().getTitle(),
                chatMessage.getSender().getNickname() + ": " + content,
                Map.of("type", CHAT_PUSH_TYPE,
                        "appointmentId", String.valueOf(chatMessage.getAppointment().getId()),
                        "messageId", String.valueOf(chatMessage.getId())));
    }
}
