package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import com.jigmnnaga.domain.repository.AppointmentParticipantRepository;
import java.time.LocalDate;
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

    public Optional<AppointmentParticipant> findAcceptedByAppointmentIdAndMemberId(Long appointmentId, Long memberId) {
        return appointmentParticipantRepository.findByAppointmentIdAndMemberId(appointmentId, memberId)
                .filter(participant -> participant.getStatus() == ParticipationStatus.ACCEPTED);
    }

    public List<AppointmentParticipant> findAllAcceptedWithMemberByAppointmentId(Long appointmentId) {
        return appointmentParticipantRepository.findAllWithMemberByAppointmentIdAndStatus(
                appointmentId, ParticipationStatus.ACCEPTED);
    }

    public List<AppointmentParticipant> findAllWithMemberByAppointment(Appointment appointment) {
        return appointmentParticipantRepository.findAllWithMemberByAppointment(appointment);
    }

    public List<AppointmentParticipant> findAllWithAppointmentByMemberId(Long memberId) {
        return appointmentParticipantRepository.findAllWithAppointmentByMemberId(memberId);
    }

    public List<AppointmentParticipant> findAllPendingWithAppointmentByMemberId(Long memberId) {
        return appointmentParticipantRepository.findAllWithAppointmentByMemberIdAndStatus(
                memberId, ParticipationStatus.PENDING);
    }

    public List<AppointmentParticipant> findAllAcceptedWithAppointmentByMemberId(Long memberId) {
        return appointmentParticipantRepository.findAllWithAppointmentByMemberIdAndStatus(
                memberId, ParticipationStatus.ACCEPTED);
    }

    public List<AppointmentParticipant> findAllAcceptedWithAppointmentByMemberIdAndConfirmedDateBetween(
            Long memberId, LocalDate startDate, LocalDate endDate) {
        return appointmentParticipantRepository.findAllWithAppointmentByMemberIdAndStatusAndConfirmedDateBetween(
                memberId, ParticipationStatus.ACCEPTED, startDate, endDate);
    }
}
