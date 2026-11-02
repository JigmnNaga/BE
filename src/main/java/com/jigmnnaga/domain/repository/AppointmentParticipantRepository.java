package com.jigmnnaga.domain.repository;

import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.Member;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentParticipantRepository extends JpaRepository<AppointmentParticipant, Long> {

    Optional<AppointmentParticipant> findByAppointmentAndMember(Appointment appointment, Member member);

    @Query("select p from AppointmentParticipant p join fetch p.member where p.appointment = :appointment order by p.id")
    List<AppointmentParticipant> findAllWithMemberByAppointment(@Param("appointment") Appointment appointment);
}
