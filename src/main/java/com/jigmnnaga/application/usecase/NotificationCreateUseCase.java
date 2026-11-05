package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.event.NotificationEvent;
import com.jigmnnaga.application.mapper.NotificationMapper;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.Notification;
import com.jigmnnaga.domain.service.MemberGetService;
import com.jigmnnaga.domain.service.NotificationSaveService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationCreateUseCase {

    private static final String NOTIFICATION_QUEUE = "/queue/notifications";

    private final MemberGetService memberGetService;
    private final NotificationSaveService notificationSaveService;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotifications(NotificationEvent event) {
        if (event.recipientIds().isEmpty()) {
            return;
        }

        List<Member> recipients = memberGetService.findAllByIds(event.recipientIds()).stream()
                .filter(member -> !member.isWithdrawn())
                .toList();
        List<Notification> notifications =
                notificationSaveService.saveAll(notificationMapper.toNotifications(event, recipients));

        notifications.forEach(notification -> messagingTemplate.convertAndSendToUser(
                String.valueOf(notification.getMember().getId()), NOTIFICATION_QUEUE,
                notificationMapper.toResponse(notification)));
    }
}
