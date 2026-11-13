package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.response.ArrivalBroadcastResponse;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.DepartureStatus;
import com.jigmnnaga.global.cache.ArrivalProximityCache;
import com.jigmnnaga.global.util.GeoUtils;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * GPS 위치 기반 자동 도착 감지 유스케이스.
 *
 * <p>조건: DEPARTED 상태이며 ACCEPTED인 참여자, 반경 100m 이내 2회 연속 관측</p>
 * <p>중복 방지: Redis SETNX (5s TTL) + 트랜잭션 롤백 시 클레임 해제</p>
 * <p>브로드캐스트: 트랜잭션 커밋 후 afterCommit()에서 WebSocket 전송</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArrivalDetectUseCase {

    static final double ARRIVAL_RADIUS_METERS = 100.0;
    static final int REQUIRED_CONSECUTIVE_COUNT = 2;

    private static final String ARRIVAL_TOPIC_PREFIX = "/sub/appointments/";
    private static final String ARRIVAL_TOPIC_SUFFIX = "/arrival";

    private final ArrivalProximityCache arrivalProximityCache;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 현재 위치를 기준으로 도착 여부를 감지합니다.
     *
     * <p>반경 내: 연속 카운트 증가 → 2회 이상이면 arrive() 호출 (호출자의 @Transactional 내에서 실행)</p>
     * <p>반경 외: 연속 카운트 초기화</p>
     *
     * @return true: 도착 처리됨, false: 아직 미도착
     */
    public boolean detectAndArrive(AppointmentParticipant participant, Long appointmentId,
                                   double currentLat, double currentLon, LocalDateTime now) {
        // ARRIVED 상태이면 감지 불필요
        if (participant.getDepartureStatus() == DepartureStatus.ARRIVED) {
            return true;
        }

        double destLat = participant.getAppointment().getDestinationLatitude();
        double destLon = participant.getAppointment().getDestinationLongitude();
        double distance = GeoUtils.distanceMeters(currentLat, currentLon, destLat, destLon);

        Long memberId = participant.getMember().getId();

        if (distance > ARRIVAL_RADIUS_METERS) {
            arrivalProximityCache.resetCount(appointmentId, memberId);
            return false;
        }

        long count = arrivalProximityCache.incrementCount(appointmentId, memberId);
        if (count < REQUIRED_CONSECUTIVE_COUNT) {
            return false;
        }

        // 중복 도착 처리 방지 (원자적 SETNX)
        if (!arrivalProximityCache.tryClaimArrival(appointmentId, memberId)) {
            log.debug("도착 처리 중복 — 이미 클레임됨. memberId={}, appointmentId={}", memberId, appointmentId);
            return false;
        }

        try {
            participant.arrive(); // JPA dirty checking → @Transactional 커밋 시 자동 반영
            arrivalProximityCache.resetCount(appointmentId, memberId);

            // 커밋 성공 후 브로드캐스트 (롤백 시 실행되지 않음)
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend(
                            ARRIVAL_TOPIC_PREFIX + appointmentId + ARRIVAL_TOPIC_SUFFIX,
                            new ArrivalBroadcastResponse(memberId, now));
                }

                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        arrivalProximityCache.releaseClaim(appointmentId, memberId);
                    }
                }
            });
            return true;
        } catch (Exception e) {
            arrivalProximityCache.releaseClaim(appointmentId, memberId);
            throw e;
        }
    }
}
