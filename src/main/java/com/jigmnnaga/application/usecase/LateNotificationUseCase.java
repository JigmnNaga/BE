package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.event.AppointmentEventFactory;
import com.jigmnnaga.application.event.AppointmentEventPublisher;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AttendanceStatus;
import com.jigmnnaga.domain.entity.DepartureStatus;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import com.jigmnnaga.domain.repository.AppointmentParticipantRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지각 예상 알림을 약속당 1회 발송하는 유스케이스.
 *
 * <p>EtaCalculateUseCase는 @Transactional이 없으므로 직접 publishEvent()를 호출하면
 * @TransactionalEventListener(AFTER_COMMIT)이 동작하지 않습니다.
 * 이 클래스를 별도 Spring 빈으로 주입받아 호출하면 REQUIRES_NEW 트랜잭션이
 * 프록시를 통해 적용되어 커밋 이후 알림이 처리됩니다.</p>
 *
 * <p>중복 발송 방지: markLateNotifSentIfNotYet()는 조건부 UPDATE로 동시 요청이
 * 발생해도 DB에서 단 한 번만 성공합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LateNotificationUseCase {

    private final AppointmentParticipantRepository appointmentParticipantRepository;
    private final AppointmentEventPublisher appointmentEventPublisher;
    private final AppointmentEventFactory appointmentEventFactory;

    /**
     * 지각 예상 알림이 아직 발송되지 않은 경우에만 1회 발송합니다.
     *
     * <p>호출 전에 EtaCalculateUseCase에서 latenessCache.putIfNewer()가 true를 반환한
     * 경우에만 호출해야 합니다. 오래된 ETA 결과는 호출 측에서 걸러집니다.</p>
     *
     * @param appointment EtaCalculateUseCase에서 이미 로딩된 Appointment (detached 허용)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendIfNeeded(Long memberId, Long appointmentId, Appointment appointment,
                             int latenessMinutes, LocalDateTime now) {
        // 취소 또는 완료된 약속이거나 약속 시각이 이미 지난 경우 발송하지 않음
        if (appointment.isCanceled() || appointment.getCompletedAt() != null) {
            return;
        }
        LocalDateTime confirmedDateTime = appointment.getConfirmedDateTime();
        if (confirmedDateTime == null || !now.isBefore(confirmedDateTime)) {
            return;
        }

        // 조건부 UPDATE: lateNotifSentAt IS NULL + ACCEPTED + ATTENDING + DEPARTED 조건을 한 번에 검사
        // 동시 위치 업데이트가 발생해도 UPDATE 성공은 단 한 번만 보장됨
        int updated = appointmentParticipantRepository.markLateNotifSentIfNotYet(
                appointmentId, memberId, now,
                ParticipationStatus.ACCEPTED, AttendanceStatus.ATTENDING, DepartureStatus.DEPARTED);

        if (updated == 0) {
            log.debug("지각 예상 알림 발송 생략 (이미 발송됐거나 대상 조건 불충족). memberId={}, appointmentId={}",
                    memberId, appointmentId);
            return;
        }

        appointmentEventPublisher.publishNotification(
                appointmentEventFactory.lateExpected(appointment, latenessMinutes, memberId));
    }
}
