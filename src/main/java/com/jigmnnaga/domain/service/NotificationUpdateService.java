package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.repository.NotificationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationUpdateService {

    private final NotificationRepository notificationRepository;

    public int markAllAsRead(Long memberId, LocalDateTime readAt) {
        return notificationRepository.markAllAsRead(memberId, readAt);
    }
}
