package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.event.PushEvent;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.NotificationType;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.domain.service.DeviceTokenDeleteService;
import com.jigmnnaga.domain.service.DeviceTokenGetService;
import com.jigmnnaga.domain.service.MemberGetService;
import com.jigmnnaga.global.client.fcm.FcmClient;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushSendUseCase {

    private final FcmClient fcmClient;
    private final DeviceTokenGetService deviceTokenGetService;
    private final DeviceTokenDeleteService deviceTokenDeleteService;
    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final MemberGetService memberGetService;

    @Async("pushExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendPush(PushEvent event) {
        if (!fcmClient.isEnabled() || event.recipientIds().isEmpty()) {
            return;
        }

        List<Long> recipientIds = excludeMutedMembers(event);
        if (recipientIds.isEmpty()) {
            return;
        }

        recipientIds = filterByNotificationSettings(recipientIds, event);
        if (recipientIds.isEmpty()) {
            return;
        }

        List<String> tokens = deviceTokenGetService.findAllTokensByMemberIds(recipientIds);
        List<String> invalidTokens = fcmClient.send(tokens, event.title(), event.body(), event.data(), false);
        if (!invalidTokens.isEmpty()) {
            deviceTokenDeleteService.deleteAllByTokens(invalidTokens);
            log.info("만료되거나 잘못된 기기 토큰 삭제 count={}", invalidTokens.size());
        }
    }

    private List<Long> excludeMutedMembers(PushEvent event) {
        if (event.appointmentId() == null) {
            return event.recipientIds();
        }
        Set<Long> mutedMemberIds = Set.copyOf(
                appointmentParticipantGetService.findAllMutedMemberIdsByAppointmentId(event.appointmentId()));
        return event.recipientIds().stream()
                .filter(memberId -> !mutedMemberIds.contains(memberId))
                .toList();
    }

    private List<Long> filterByNotificationSettings(List<Long> recipientIds, PushEvent event) {
        NotificationType type = resolveNotificationType(event);
        if (type == null) {
            return recipientIds;
        }
        Map<Long, Member> memberMap = memberGetService.findAllByIds(recipientIds).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));
        return recipientIds.stream()
                .filter(id -> {
                    Member member = memberMap.get(id);
                    return member == null || member.isPushAllowedFor(type);
                })
                .toList();
    }

    private NotificationType resolveNotificationType(PushEvent event) {
        String typeName = event.data() != null ? event.data().get("type") : null;
        if (typeName == null) {
            return null;
        }
        try {
            return NotificationType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 알림 유형, 필터링 건너뜀: type={}", typeName);
            return null;
        }
    }
}
