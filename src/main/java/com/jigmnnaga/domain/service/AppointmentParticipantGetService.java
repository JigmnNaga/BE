package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.repository.AppointmentParticipantRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppointmentParticipantGetService {

    private final AppointmentParticipantRepository appointmentParticipantRepository;

    public Optional<AppointmentParticipant> findByAppointmentAndMember(Appointment appointment, Member member) {
        return appointmentParticipantRepository.findByAppointmentAndMember(appointment, member);
    }

    public List<AppointmentParticipant> findAllWithMemberByAppointment(Appointment appointment) {
        return appointmentParticipantRepository.findAllWithMemberByAppointment(appointment);
    }
}
