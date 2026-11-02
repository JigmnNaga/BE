package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.request.AppointmentCreateRequest;
import com.jigmnnaga.application.dto.response.AppointmentCreateResponse;
import com.jigmnnaga.application.dto.response.AppointmentDetailResponse;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentCandidateDate;
import com.jigmnnaga.domain.entity.AppointmentCandidateLocation;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.ParticipantRole;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import java.time.LocalDate;
import java.util.Comparator;
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

    public AppointmentDetailResponse toDetailResponse(Appointment appointment, List<AppointmentParticipant> participants,
                                                      AppointmentParticipant me, String inviteUrl) {
        List<AppointmentDetailResponse.MemberStatusResponse> members = participants.stream()
                .sorted(Comparator.comparing((AppointmentParticipant participant) -> !participant.isHost()))
                .map(participant -> AppointmentDetailResponse.MemberStatusResponse.builder()
                        .memberId(participant.getMember().getId())
                        .nickname(participant.getMember().getNickname())
                        .profileImageUrl(participant.getMember().getProfileImageUrl())
                        .role(participant.getRole())
                        .scheduleSubmitted(participant.hasSubmittedSchedule())
                        .build())
                .toList();

        int scheduleSubmittedCount = (int) participants.stream()
                .filter(AppointmentParticipant::hasSubmittedSchedule)
                .count();

        return AppointmentDetailResponse.builder()
                .appointmentId(appointment.getId())
                .title(appointment.getTitle())
                .dateSelectionType(appointment.getDateSelectionType())
                .candidateDates(appointment.getCandidateDates().stream()
                        .map(AppointmentCandidateDate::getCandidateDate)
                        .sorted()
                        .toList())
                .candidateLocations(appointment.getCandidateLocations().stream()
                        .map(this::toCandidateLocationResponse)
                        .toList())
                .responseDeadline(appointment.getResponseDeadline())
                .inviteUrl(inviteUrl)
                .confirmed(appointment.isConfirmed())
                .confirmedDate(appointment.getConfirmedDate())
                .confirmedTime(appointment.getConfirmedTime())
                .confirmedLocation(appointment.getConfirmedLocation() == null
                        ? null
                        : toCandidateLocationResponse(appointment.getConfirmedLocation()))
                .detailPlace(toDetailPlaceResponse(appointment))
                .myRole(me.getRole())
                .joinedCount(participants.size())
                .scheduleSubmittedCount(scheduleSubmittedCount)
                .members(members)
                .build();
    }

    private AppointmentDetailResponse.CandidateLocationResponse toCandidateLocationResponse(
            AppointmentCandidateLocation location) {
        return AppointmentDetailResponse.CandidateLocationResponse.builder()
                .locationId(location.getId())
                .placeName(location.getPlaceName())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
    }

    private AppointmentDetailResponse.DetailPlaceResponse toDetailPlaceResponse(Appointment appointment) {
        if (appointment.getDetailPlaceName() == null) {
            return null;
        }
        return AppointmentDetailResponse.DetailPlaceResponse.builder()
                .placeName(appointment.getDetailPlaceName())
                .address(appointment.getDetailPlaceAddress())
                .latitude(appointment.getDetailPlaceLatitude())
                .longitude(appointment.getDetailPlaceLongitude())
                .build();
    }
}
