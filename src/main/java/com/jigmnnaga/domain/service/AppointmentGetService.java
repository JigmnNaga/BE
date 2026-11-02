package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppointmentGetService {

    private final AppointmentRepository appointmentRepository;

    public boolean existsByInviteCode(String inviteCode) {
        return appointmentRepository.existsByInviteCode(inviteCode);
    }
}
