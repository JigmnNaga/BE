package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.response.CalendarResponse;
import com.jigmnnaga.application.dto.response.MyAppointmentResponse;
import com.jigmnnaga.application.dto.response.MyAppointmentResponse.MemberProfileResponse;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MyAppointmentMapper {

    private final ProfileImageUrlResolver profileImageUrlResolver;

    public MyAppointmentResponse toMyAppointmentResponse(Appointment appointment, Long memberId,
                                                         Map<Long, Long> unreadCounts) {
        return MyAppointmentResponse.builder()
                .appointmentId(appointment.getId())
                .title(appointment.getTitle())
                .canceled(appointment.isCanceled())
                .confirmed(appointment.isConfirmed())
                .confirmedDate(appointment.getConfirmedDate())
                .confirmedTime(appointment.getConfirmedTime())
                .confirmedLocationName(appointment.getConfirmedLocation() == null
                        ? null
                        : appointment.getConfirmedLocation().getPlaceName())
                .detailPlaceName(appointment.getDetailPlaceName())
                .candidateStartDate(appointment.getFirstCandidateDate())
                .candidateEndDate(appointment.getLastCandidateDate())
                .memberCount(appointment.getAcceptedMemberCount())
                .participants(toParticipantProfiles(appointment, memberId))
                .unreadMessageCount(unreadCounts.getOrDefault(appointment.getId(), 0L).intValue())
                .build();
    }

    // 목록 카드의 프로필 묶음: 나 → 방장 → 참여한 순서
    private List<MemberProfileResponse> toParticipantProfiles(Appointment appointment, Long memberId) {
        return appointment.getParticipants().stream()
                .filter(participant -> participant.getStatus() == ParticipationStatus.ACCEPTED)
                .sorted(Comparator
                        .comparing((AppointmentParticipant participant) ->
                                !participant.getMember().getId().equals(memberId))
                        .thenComparing(participant -> !participant.isHost())
                        .thenComparing(AppointmentParticipant::getId))
                .map(participant -> MemberProfileResponse.builder()
                        .memberId(participant.getMember().getId())
                        .nickname(participant.getMember().getNickname())
                        .profileImageUrl(profileImageUrlResolver.resolve(participant.getMember()))
                        .profileColor(participant.getMember().getProfileColor())
                        .build())
                .toList();
    }

    public CalendarResponse toCalendarResponse(List<Appointment> appointments, Long memberId,
                                               Map<Long, Long> unreadCounts) {
        Map<LocalDate, List<Appointment>> appointmentsByDate = appointments.stream()
                .collect(Collectors.groupingBy(Appointment::getConfirmedDate, TreeMap::new, Collectors.toList()));

        List<CalendarResponse.CalendarDayResponse> days = appointmentsByDate.entrySet().stream()
                .map(entry -> CalendarResponse.CalendarDayResponse.builder()
                        .date(entry.getKey())
                        .appointments(entry.getValue().stream()
                                .sorted(Comparator.comparing(Appointment::getConfirmedTime))
                                .map(appointment -> toMyAppointmentResponse(appointment, memberId, unreadCounts))
                                .toList())
                        .build())
                .toList();

        return CalendarResponse.builder()
                .days(days)
                .build();
    }
}
