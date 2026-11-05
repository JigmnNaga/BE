package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationDeleteService {

    private final NotificationRepository notificationRepository;

    public void deleteAllByMemberId(Long memberId) {
        notificationRepository.deleteAllByMemberId(memberId);
    }
}
