package com.jigmnnaga.application.event;

import com.jigmnnaga.application.dto.response.LocationSharingEndedResponse;
import com.jigmnnaga.application.dto.response.LocationSharingEndedResponse.Reason;
import com.jigmnnaga.global.cache.LocationCache;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class LocationSharingEndPublisher {

    private static final String TOPIC_PREFIX = "/sub/appointments/";
    private static final String TOPIC_SUFFIX = "/location-end";

    private final LocationCache locationCache;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 커밋이 성공하면 최신 위치 캐시를 지우고, 다른 참여자가 지도에서 마커를 내리도록 종료 이벤트를 보낸다.
     */
    public void publishAfterCommit(Long appointmentId, Long memberId, Reason reason, LocalDateTime endedAt) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(appointmentId, memberId, reason, endedAt);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(appointmentId, memberId, reason, endedAt);
            }
        });
    }

    private void publish(Long appointmentId, Long memberId, Reason reason, LocalDateTime endedAt) {
        locationCache.remove(appointmentId, memberId);
        messagingTemplate.convertAndSend(TOPIC_PREFIX + appointmentId + TOPIC_SUFFIX,
                new LocationSharingEndedResponse(memberId, reason, endedAt));
    }
}
