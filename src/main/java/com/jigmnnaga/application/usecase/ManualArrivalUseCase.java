package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.response.ArrivalBroadcastResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.global.cache.ArrivalProximityCache;
import com.jigmnnaga.global.util.TimeZones;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 사용자가 직접 도착 버튼을 눌렀을 때 처리하는 수동 도착 유스케이스.
 *
 * <p>상태 전이: DEPARTED → ARRIVED</p>
 * <p>중복 방지: ArrivalDetectUseCase와 동일한 Redis SETNX 방식으로 자동 도착과 충돌을 차단합니다.</p>
 * <p>브로드캐스트: 트랜잭션 커밋 후 afterCommit()에서 WebSocket 전송</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ManualArrivalUseCase {

    private static final String ARRIVAL_TOPIC_PREFIX = "/sub/appointments/";
    private static final String ARRIVAL_TOPIC_SUFFIX = "/arrival";

    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final ArrivalProximityCache arrivalProximityCache;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void arrive(Long memberId, Long appointmentId) {
        AppointmentParticipant me = appointmentParticipantGetService
                .findAcceptedByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));

        Appointment appointment = me.getAppointment();
        if (appointment.isCanceled()) {
            throw new BusinessException(ErrorCode.APPOINTMENT_CANCELED);
        }
        if (!appointment.isConfirmed()) {
            throw new BusinessException(ErrorCode.TRAVEL_DESTINATION_NOT_CONFIRMED);
        }

        // 자동 도착 감지(ArrivalDetectUseCase)와 동일한 Redis SETNX로 동시 처리 방지
        if (!arrivalProximityCache.tryClaimArrival(appointmentId, memberId)) {
            log.debug("수동 도착 처리 중복 — 이미 클레임됨. memberId={}, appointmentId={}", memberId, appointmentId);
            throw new BusinessException(ErrorCode.ALREADY_ARRIVED);
        }

        try {
            // BEFORE_DEPARTURE → NOT_YET_DEPARTED(400), ARRIVED → ALREADY_ARRIVED(409)
            me.arrive();
            arrivalProximityCache.resetCount(appointmentId, memberId);

            LocalDateTime now = LocalDateTime.now(TimeZones.SEOUL);

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
        } catch (Exception e) {
            arrivalProximityCache.releaseClaim(appointmentId, memberId);
            throw e;
        }
    }
}
