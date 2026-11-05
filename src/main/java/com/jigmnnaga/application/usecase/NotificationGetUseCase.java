package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.response.NotificationCountResponse;
import com.jigmnnaga.application.dto.response.NotificationPageResponse;
import com.jigmnnaga.application.mapper.NotificationMapper;
import com.jigmnnaga.domain.service.NotificationGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationGetUseCase {

    private final NotificationGetService notificationGetService;
    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(Long memberId, Long cursor, int size) {
        return notificationMapper.toPageResponse(
                notificationGetService.findAllBeforeCursor(memberId, cursor, size + 1), size);
    }

    @Transactional(readOnly = true)
    public NotificationCountResponse getUnreadCount(Long memberId) {
        return NotificationCountResponse.builder()
                .count(notificationGetService.countUnread(memberId))
                .build();
    }
}
