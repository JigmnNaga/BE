package com.jigmnnaga.domain.repository;

import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentParticipantRepository extends JpaRepository<AppointmentParticipant, Long> {

    Optional<AppointmentParticipant> findByAppointmentAndMember(Appointment appointment, Member member);

    Optional<AppointmentParticipant> findByAppointmentIdAndMemberId(Long appointmentId, Long memberId);

    @Query("select p.member.id from AppointmentParticipant p "
            + "where p.appointment.id = :appointmentId and p.notificationEnabled = false")
    List<Long> findAllMutedMemberIdsByAppointmentId(@Param("appointmentId") Long appointmentId);

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
