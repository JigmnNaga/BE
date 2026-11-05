package com.jigmnnaga.domain.repository;

import com.jigmnnaga.domain.entity.Appointment;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByInviteCode(String inviteCode);

    Optional<Appointment> findByInviteCode(String inviteCode);

    List<Appointment> findAllByCanceledFalseAndDeadlineReminderSentAtIsNullAndResponseDeadlineBetween(
            LocalDateTime from, LocalDateTime to);

    List<Appointment> findAllByCanceledFalseAndDeadlineClosedNotifiedAtIsNullAndResponseDeadlineBetween(
            LocalDateTime from, LocalDateTime to);

    List<Appointment> findAllByCanceledFalseAndPreAppointmentNotifiedAtIsNullAndConfirmedDateBetween(
            LocalDate from, LocalDate to);
}
