package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.Notification;
import com.jigmnnaga.domain.repository.NotificationRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationGetService {

    private final NotificationRepository notificationRepository;

    public List<Notification> findAllBeforeCursor(Long memberId, Long cursor, int limit) {
        return notificationRepository.findAllByMemberIdBeforeCursor(memberId, cursor, PageRequest.of(0, limit));
    }

    public Optional<Notification> findByIdAndMemberId(Long notificationId, Long memberId) {
        return notificationRepository.findByIdAndMemberId(notificationId, memberId);
    }

    public long countUnread(Long memberId) {
        return notificationRepository.countByMemberIdAndReadAtIsNull(memberId);
    }
}
