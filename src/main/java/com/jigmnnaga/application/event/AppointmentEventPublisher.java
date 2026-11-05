package com.jigmnnaga.application.event;

import com.jigmnnaga.application.mapper.ChatMessageMapper;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.ChatMessage;
import com.jigmnnaga.domain.service.ChatMessageSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ChatMessageSaveService chatMessageSaveService;
    private final ChatMessageMapper chatMessageMapper;

    public void publishNotification(NotificationEvent event) {
        if (!event.recipientIds().isEmpty()) {
            applicationEventPublisher.publishEvent(event);
        }
    }

    public void sendSystemMessage(Appointment appointment, String content) {
        ChatMessage chatMessage = chatMessageSaveService.save(chatMessageMapper.toSystemMessage(appointment, content));
        applicationEventPublisher.publishEvent(new ChatMessageCreatedEvent(chatMessageMapper.toResponse(chatMessage, null)));
    }
}
