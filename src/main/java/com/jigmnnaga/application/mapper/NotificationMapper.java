package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.response.NotificationPageResponse;
import com.jigmnnaga.application.dto.response.NotificationResponse;
import com.jigmnnaga.application.event.NotificationEvent;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.Notification;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public List<Notification> toNotifications(NotificationEvent event, List<Member> recipients) {
        return recipients.stream()
                .map(recipient -> Notification.builder()
                        .member(recipient)
                        .appointmentId(event.appointmentId())
                        .type(event.type())
                        .title(event.title())
                        .body(event.body())
                        .build())
                .toList();
    }

    public NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .appointmentId(notification.getAppointmentId())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    public NotificationPageResponse toPageResponse(List<Notification> newestFirst, int size) {
        boolean hasNext = newestFirst.size() > size;
        List<Notification> page = newestFirst.subList(0, Math.min(size, newestFirst.size()));

        return NotificationPageResponse.builder()
                .notifications(page.stream().map(this::toResponse).toList())
                .nextCursor(hasNext ? page.getLast().getId() : null)
                .hasNext(hasNext)
                .build();
    }
}
