
package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.repository.AppointmentRepository;
import com.jigmnnaga.global.cache.InviteCodeCache;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppointmentGetService {

    private final AppointmentRepository appointmentRepository;
    private final InviteCodeCache inviteCodeCache;

    public boolean existsByInviteCode(String inviteCode) {
        return appointmentRepository.existsByInviteCode(inviteCode);
    }

    public Optional<Appointment> findByInviteCode(String inviteCode) {

        // 1. Redis에서 초대 코드에 해당하는 약속 ID 조회
        Optional<Long> cachedId = inviteCodeCache.get(inviteCode);

        if (cachedId.isPresent()) {
            Optional<Appointment> cachedAppointment =
                    appointmentRepository.findById(cachedId.get());

            // 2. 캐시된 약속 ID와 초대 코드가 일치하면 반환
            if (cachedAppointment.isPresent()
                    && inviteCode.equals(cachedAppointment.get().getInviteCode())) {
                return cachedAppointment;
            }

            // 3. 유효하지 않은 캐시 삭제
            inviteCodeCache.evict(inviteCode);
        }

        // 4. 캐시가 없으면 기존 DB 조회
        Optional<Appointment> appointment =
                appointmentRepository.findByInviteCode(inviteCode);

        // 5. 조회 성공 시 Redis에 저장
        appointment.ifPresent(value ->
                inviteCodeCache.put(inviteCode, value.getId())
        );

        return appointment;
    }

    public List<Appointment> findAllNeedingDeadlineReminder(LocalDateTime from, LocalDateTime to) {
        return appointmentRepository
                .findAllByCanceledFalseAndDeadlineReminderSentAtIsNullAndResponseDeadlineBetween(from, to);
    }

    public List<Appointment> findAllNeedingDeadlineClosedNotification(LocalDateTime from, LocalDateTime to) {
        return appointmentRepository
                .findAllByCanceledFalseAndDeadlineClosedNotifiedAtIsNullAndResponseDeadlineBetween(from, to);
    }

    public List<Appointment> findAllNeedingPreAppointmentNotification(LocalDate from, LocalDate to) {
        return appointmentRepository
                .findAllByCanceledFalseAndPreAppointmentNotifiedAtIsNullAndConfirmedDateBetween(from, to);
    }
}