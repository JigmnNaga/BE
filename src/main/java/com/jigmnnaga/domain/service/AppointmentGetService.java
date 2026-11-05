package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.repository.AppointmentRepository;
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

    public boolean existsByInviteCode(String inviteCode) {
        return appointmentRepository.existsByInviteCode(inviteCode);
    }

    public Optional<Appointment> findByInviteCode(String inviteCode) {
        return appointmentRepository.findByInviteCode(inviteCode);
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
