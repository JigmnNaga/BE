package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.response.CalendarResponse;
import com.jigmnnaga.application.dto.response.MyAppointmentResponse;
import com.jigmnnaga.domain.entity.Appointment;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MyAppointmentMapper {

    public MyAppointmentResponse toMyAppointmentResponse(Appointment appointment, Map<Long, Long> unreadCounts) {
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
                .unreadMessageCount(unreadCounts.getOrDefault(appointment.getId(), 0L).intValue())
                .build();
    }

    public CalendarResponse toCalendarResponse(List<Appointment> appointments, Map<Long, Long> unreadCounts) {
        Map<LocalDate, List<Appointment>> appointmentsByDate = appointments.stream()
                .collect(Collectors.groupingBy(Appointment::getConfirmedDate, TreeMap::new, Collectors.toList()));

        List<CalendarResponse.CalendarDayResponse> days = appointmentsByDate.entrySet().stream()
                .map(entry -> CalendarResponse.CalendarDayResponse.builder()
                        .date(entry.getKey())
                        .appointments(entry.getValue().stream()
                                .sorted(Comparator.comparing(Appointment::getConfirmedTime))
                                .map(appointment -> toMyAppointmentResponse(appointment, unreadCounts))
                                .toList())
                        .build())
                .toList();

        return CalendarResponse.builder()
                .days(days)
                .build();
    }
}
