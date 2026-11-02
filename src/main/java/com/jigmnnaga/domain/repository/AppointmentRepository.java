package com.jigmnnaga.domain.repository;

import com.jigmnnaga.domain.entity.Appointment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByInviteCode(String inviteCode);

    Optional<Appointment> findByInviteCode(String inviteCode);
}
