package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppointmentDeleteService {

    private final AppointmentRepository appointmentRepository;

    public void delete(Appointment appointment) {
        appointmentRepository.delete(appointment);
    }
}
