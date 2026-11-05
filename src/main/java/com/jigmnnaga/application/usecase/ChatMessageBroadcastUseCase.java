package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.event.ChatMessageCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatMessageBroadcastUseCase {

    private static final String APPOINTMENT_TOPIC_PREFIX = "/sub/appointments/";

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void broadcast(ChatMessageCreatedEvent event) {
        messagingTemplate.convertAndSend(APPOINTMENT_TOPIC_PREFIX + event.message().appointmentId(), event.message());
    }
}
