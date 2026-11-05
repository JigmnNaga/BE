package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.AppointmentListType;
import com.jigmnnaga.application.dto.response.CalendarResponse;
import com.jigmnnaga.application.dto.response.MyAppointmentResponse;
import com.jigmnnaga.application.mapper.MyAppointmentMapper;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.domain.service.ChatMessageGetService;
import com.jigmnnaga.global.util.TimeZones;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MyAppointmentGetUseCase {

    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final MyAppointmentMapper myAppointmentMapper;
    private final ChatMessageGetService chatMessageGetService;

    @Transactional(readOnly = true)
    public List<MyAppointmentResponse> getMyAppointments(Long memberId, AppointmentListType type) {
        LocalDateTime now = LocalDateTime.now(TimeZones.SEOUL);
        boolean isUpcoming = type == AppointmentListType.UPCOMING;

        Comparator<Appointment> comparator = Comparator.comparing(Appointment::getScheduledDateTime);
        if (!isUpcoming) {
            comparator = comparator.reversed();
        }

        Map<Long, Long> unreadCounts = chatMessageGetService.countUnreadTextMessagesByAppointment(memberId);
        return getMyAcceptedAppointments(memberId).stream()
                .filter(appointment -> appointment.isUpcoming(now) == isUpcoming)
                .sorted(comparator)
                .map(appointment -> myAppointmentMapper.toMyAppointmentResponse(appointment, unreadCounts))
                .toList();
    }

    @Transactional(readOnly = true)
    public MyAppointmentResponse getNextAppointment(Long memberId) {
        LocalDateTime now = LocalDateTime.now(TimeZones.SEOUL);

        return getMyAcceptedAppointments(memberId).stream()
                .filter(appointment -> !appointment.isCanceled())
                .filter(appointment -> appointment.getConfirmedDate() != null && appointment.isUpcoming(now))
                .min(Comparator.comparing(Appointment::getConfirmedDateTime))
                .map(appointment -> myAppointmentMapper.toMyAppointmentResponse(
                        appointment, chatMessageGetService.countUnreadTextMessagesByAppointment(memberId)))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public CalendarResponse getCalendar(Long memberId, YearMonth month) {
        List<Appointment> appointments = appointmentParticipantGetService
                .findAllAcceptedWithAppointmentByMemberIdAndConfirmedDateBetween(
                        memberId, month.atDay(1), month.atEndOfMonth())
                .stream()
                .map(AppointmentParticipant::getAppointment)
                .filter(appointment -> !appointment.isCanceled())
                .toList();

        return myAppointmentMapper.toCalendarResponse(
                appointments, chatMessageGetService.countUnreadTextMessagesByAppointment(memberId));
    }

    private List<Appointment> getMyAcceptedAppointments(Long memberId) {
        return appointmentParticipantGetService.findAllAcceptedWithAppointmentByMemberId(memberId).stream()
                .map(AppointmentParticipant::getAppointment)
                .toList();
    }
}
