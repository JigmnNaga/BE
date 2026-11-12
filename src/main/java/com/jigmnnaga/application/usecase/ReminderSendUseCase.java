package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.response.ReminderSendResponse;
import com.jigmnnaga.application.dto.response.ReminderSendResponse.ReminderResultResponse;
import com.jigmnnaga.application.event.AppointmentEventFactory;
import com.jigmnnaga.application.event.AppointmentEventPublisher;
import com.jigmnnaga.application.event.NotificationEvent;
import com.jigmnnaga.application.validator.AppointmentAccessValidator;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.AttendanceStatus;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReminderSendUseCase {

    private final AppointmentAccessValidator appointmentAccessValidator;
    private final AppointmentEventPublisher appointmentEventPublisher;
    private final AppointmentEventFactory appointmentEventFactory;

    @Transactional
    public ReminderSendResponse sendReminder(Long memberId, Long appointmentId) {
        Appointment appointment = appointmentAccessValidator.getHostAppointment(memberId, appointmentId);
        appointmentAccessValidator.validateNotCanceled(appointment);

        List<AppointmentParticipant> others = appointment.getParticipants().stream()
                .filter(participant -> participant.getStatus() == ParticipationStatus.ACCEPTED)
                .filter(participant -> !participant.getMember().getId().equals(memberId))
                .toList();

        List<NotificationEvent> events = appointment.isConfirmed()
                ? List.of(
                        appointmentEventFactory.attendanceRequested(appointment, memberIdsOf(others.stream()
                                .filter(participant -> participant.getAttendanceStatus() == AttendanceStatus.PENDING)
                                .toList())),
                        appointmentEventFactory.departureInputRequested(appointment, memberIdsOf(others.stream()
                                .filter(participant -> participant.getAttendanceStatus() == AttendanceStatus.ATTENDING)
                                .filter(participant -> !participant.hasDepartureLocation())
                                .toList())))
                : List.of(appointmentEventFactory.scheduleInputRequested(appointment, memberIdsOf(others.stream()
                        .filter(participant -> !participant.hasSubmittedSchedule())
                        .toList())));
        events.forEach(appointmentEventPublisher::publishNotification);

        List<ReminderResultResponse> reminders = events.stream()
                .map(event -> ReminderResultResponse.builder()
                        .type(event.type())
                        .sentCount(event.recipientIds().size())
                        .build())
                .toList();
        return ReminderSendResponse.builder()
                .sentCount(reminders.stream().mapToInt(ReminderResultResponse::sentCount).sum())
                .reminders(reminders)
                .build();
    }

    private List<Long> memberIdsOf(List<AppointmentParticipant> participants) {
        return participants.stream()
                .map(participant -> participant.getMember().getId())
                .toList();
    }
}
