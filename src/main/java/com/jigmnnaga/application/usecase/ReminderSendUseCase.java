package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.response.ReminderSendResponse;
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

        NotificationEvent event = appointment.isConfirmed()
                ? appointmentEventFactory.attendanceRequested(appointment, memberIdsOf(others.stream()
                        .filter(participant -> participant.getAttendanceStatus() == AttendanceStatus.PENDING)
                        .toList()))
                : appointmentEventFactory.scheduleInputRequested(appointment, memberIdsOf(others.stream()
                        .filter(participant -> !participant.hasSubmittedSchedule())
                        .toList()));
        appointmentEventPublisher.publishNotification(event);

        return ReminderSendResponse.builder()
                .type(event.type())
                .sentCount(event.recipientIds().size())
                .build();
    }

    private List<Long> memberIdsOf(List<AppointmentParticipant> participants) {
        return participants.stream()
                .map(participant -> participant.getMember().getId())
                .toList();
    }
}
