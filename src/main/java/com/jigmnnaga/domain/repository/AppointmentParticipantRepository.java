package com.jigmnnaga.domain.repository;

import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.AttendanceStatus;
import com.jigmnnaga.domain.entity.DepartureStatus;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentParticipantRepository extends JpaRepository<AppointmentParticipant, Long> {

    Optional<AppointmentParticipant> findByAppointmentAndMember(Appointment appointment, Member member);

    Optional<AppointmentParticipant> findByAppointmentIdAndMemberId(Long appointmentId, Long memberId);

    @Query("select p.member.id from AppointmentParticipant p "
            + "where p.appointment.id = :appointmentId and p.notificationEnabled = false")
    List<Long> findAllMutedMemberIdsByAppointmentId(@Param("appointmentId") Long appointmentId);

    /**
     * 지각 예상 알림 발송 이력을 원자적으로 기록합니다.
     *
     * <p>WHERE 절에 lateNotifSentAt IS NULL 과 상태 조건을 포함하므로,
     * 동시 요청이 발생해도 DB UPDATE 가 한 번만 성공합니다.</p>
     *
     * @return 갱신된 행 수 (1: 발송 처리됨, 0: 이미 발송됐거나 대상 아님)
     */
    @Modifying
    @Query("UPDATE AppointmentParticipant p SET p.lateNotifSentAt = :sentAt "
            + "WHERE p.appointment.id = :appointmentId AND p.member.id = :memberId "
            + "AND p.lateNotifSentAt IS NULL "
            + "AND p.status = :accepted AND p.attendanceStatus = :attending "
            + "AND p.departureStatus = :departed")
    int markLateNotifSentIfNotYet(
            @Param("appointmentId") Long appointmentId,
            @Param("memberId") Long memberId,
            @Param("sentAt") LocalDateTime sentAt,
            @Param("accepted") ParticipationStatus accepted,
            @Param("attending") AttendanceStatus attending,
            @Param("departed") DepartureStatus departed);

    @Query("select p from AppointmentParticipant p join fetch p.member where p.appointment = :appointment order by p.id")
    List<AppointmentParticipant> findAllWithMemberByAppointment(@Param("appointment") Appointment appointment);

    @Query("select p from AppointmentParticipant p join fetch p.member "
            + "where p.appointment.id = :appointmentId and p.status = :status order by p.id")
    List<AppointmentParticipant> findAllWithMemberByAppointmentIdAndStatus(
            @Param("appointmentId") Long appointmentId,
            @Param("status") ParticipationStatus status
    );

    @Query("select p from AppointmentParticipant p join fetch p.appointment where p.member.id = :memberId")
    List<AppointmentParticipant> findAllWithAppointmentByMemberId(@Param("memberId") Long memberId);

    @Query("select p from AppointmentParticipant p join fetch p.appointment "
            + "where p.member.id = :memberId and p.status = :status")
    List<AppointmentParticipant> findAllWithAppointmentByMemberIdAndStatus(
            @Param("memberId") Long memberId,
            @Param("status") ParticipationStatus status
    );

    @Query("select p from AppointmentParticipant p join fetch p.appointment a "
            + "where p.member.id = :memberId and p.status = :status "
            + "and a.confirmedDate between :startDate and :endDate")
    List<AppointmentParticipant> findAllWithAppointmentByMemberIdAndStatusAndConfirmedDateBetween(
            @Param("memberId") Long memberId,
            @Param("status") ParticipationStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
