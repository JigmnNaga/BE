package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.repository.AppointmentParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppointmentParticipantSaveService {

    private final AppointmentParticipantRepository appointmentParticipantRepository;

    public void save(AppointmentParticipant appointmentParticipant) {
        appointmentParticipantRepository.save(appointmentParticipant);
    }
}
