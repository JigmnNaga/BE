package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.request.AppointmentCreateRequest;
import com.jigmnnaga.application.dto.response.AppointmentCreateResponse;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentCandidateDate;
import com.jigmnnaga.domain.entity.AppointmentCandidateLocation;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.ParticipantRole;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public Appointment toAppointment(AppointmentCreateRequest request, List<LocalDate> sortedDates,
                                     Member host, String inviteCode) {
        Appointment appointment = Appointment.builder()
                .title(request.title().strip())
                .dateSelectionType(request.dateSelectionType())
                .responseDeadline(request.responseDeadline())
                .inviteCode(inviteCode)
                .build();

        sortedDates.forEach(date -> appointment.addCandidateDate(
                AppointmentCandidateDate.builder()
                        .candidateDate(date)
                        .build()));

        request.candidateLocations().forEach(location -> appointment.addCandidateLocation(
                AppointmentCandidateLocation.builder()
                        .placeName(location.placeName())
                        .address(location.address())
                        .latitude(location.latitude())
                        .longitude(location.longitude())
                        .build()));

        appointment.addParticipant(AppointmentParticipant.builder()
                .member(host)
                .role(ParticipantRole.HOST)
                .status(ParticipationStatus.ACCEPTED)
                .build());

        return appointment;
    }

    public AppointmentCreateResponse toCreateResponse(Appointment appointment, String inviteUrl) {
        return AppointmentCreateResponse.builder()
                .appointmentId(appointment.getId())
                .title(appointment.getTitle())
                .dateSelectionType(appointment.getDateSelectionType())
                .candidateDates(appointment.getCandidateDates().stream()
                        .map(AppointmentCandidateDate::getCandidateDate)
                        .toList())
                .candidateLocationNames(appointment.getCandidateLocations().stream()
                        .map(AppointmentCandidateLocation::getPlaceName)
                        .toList())
                .responseDeadline(appointment.getResponseDeadline())
                .inviteCode(appointment.getInviteCode())
                .inviteUrl(inviteUrl)
                .build();
    }
}
