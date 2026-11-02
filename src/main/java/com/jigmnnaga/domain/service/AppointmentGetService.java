package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.repository.AppointmentRepository;
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
}
