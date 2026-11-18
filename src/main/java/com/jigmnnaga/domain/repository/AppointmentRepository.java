package com.jigmnnaga.domain.repository;

import com.jigmnnaga.domain.entity.Appointment;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByInviteCode(String inviteCode);

    Optional<Appointment> findByInviteCode(String inviteCode);

    List<Appointment> findAllByCanceledFalseAndDeadlineReminderSentAtIsNullAndResponseDeadlineBetween(
            LocalDateTime from, LocalDateTime to);

    List<Appointment> findAllByCanceledFalseAndDeadlineClosedNotifiedAtIsNullAndResponseDeadlineBetween(
            LocalDateTime from, LocalDateTime to);

    List<Appointment> findAllByCanceledFalseAndPreAppointmentNotifiedAtIsNullAndConfirmedDateBetween(
            LocalDate from, LocalDate to);

    List<Appointment> findAllByCanceledFalseAndConfirmedDateBetween(LocalDate from, LocalDate to);

    @Query("SELECT a FROM Appointment a " +
           "WHERE a.canceled = false " +
           "AND a.completedAt IS NULL " +
           "AND a.confirmedDate IS NOT NULL " +
           "AND a.confirmedTime IS NOT NULL " +
           "AND a.confirmedLocation IS NOT NULL " +
           "AND a.detailPlaceName IS NOT NULL " +
           "AND (a.confirmedDate < :cutoffDate " +
           "     OR (a.confirmedDate = :cutoffDate AND a.confirmedTime <= :cutoffTime))")
    List<Appointment> findAllNeedingCompletion(
            @Param("cutoffDate") LocalDate cutoffDate,
            @Param("cutoffTime") LocalTime cutoffTime);
}
