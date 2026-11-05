package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.response.NotificationCountResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.service.NotificationGetService;
import com.jigmnnaga.domain.service.NotificationUpdateService;
import com.jigmnnaga.global.util.TimeZones;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationReadUseCase {

    private final NotificationGetService notificationGetService;
    private final NotificationUpdateService notificationUpdateService;

    @Transactional
    public void readNotification(Long memberId, Long notificationId) {
        notificationGetService.findByIdAndMemberId(notificationId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND))
                .read(LocalDateTime.now(TimeZones.SEOUL));
    }

    @Transactional
    public NotificationCountResponse readAllNotifications(Long memberId) {
        int readCount = notificationUpdateService.markAllAsRead(memberId, LocalDateTime.now(TimeZones.SEOUL));
        return NotificationCountResponse.builder()
                .count(readCount)
                .build();
    }
}
