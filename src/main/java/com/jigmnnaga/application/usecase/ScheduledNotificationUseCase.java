package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.event.AppointmentEventFactory;
import com.jigmnnaga.application.event.AppointmentEventPublisher;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.AttendanceStatus;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import com.jigmnnaga.domain.service.AppointmentGetService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ScheduledNotificationUseCase {

    private static final long DEADLINE_REMINDER_HOURS = 24;
    private static final long PRE_APPOINTMENT_HOURS = 5;
    private static final long MISSED_WINDOW_HOURS = 24;

    private final AppointmentGetService appointmentGetService;
    private final AppointmentEventPublisher appointmentEventPublisher;
    private final AppointmentEventFactory appointmentEventFactory;

    @Transactional
    public int sendDeadlineReminders(LocalDateTime now) {
        List<Appointment> appointments = appointmentGetService.findAllNeedingDeadlineReminder(
                now, now.plusHours(DEADLINE_REMINDER_HOURS));

        appointments.forEach(appointment -> {
            List<Long> recipientIds = acceptedMemberIds(appointment, participant -> !participant.hasSubmittedSchedule());
            appointmentEventPublisher.publishNotification(
                    appointmentEventFactory.voteDeadlineSoon(appointment, recipientIds));
            appointment.markDeadlineReminderSent(now);
        });
        return appointments.size();
    }

    @Transactional
    public int sendDeadlineClosedNotifications(LocalDateTime now) {
        List<Appointment> appointments = appointmentGetService.findAllNeedingDeadlineClosedNotification(
                now.minusHours(MISSED_WINDOW_HOURS), now);

        appointments.forEach(appointment -> {
            if (!appointment.isConfirmed()) {
                appointmentEventPublisher.sendSystemMessage(appointment,
                        appointmentEventFactory.responseDeadlineClosedChatMessage());
                appointment.getHostMemberId().ifPresent(hostId -> appointmentEventPublisher.publishNotification(
                        appointmentEventFactory.responseDeadlineClosed(appointment, hostId)));
            }
            appointment.markDeadlineClosedNotified(now);
        });
        return appointments.size();
    }

    @Transactional
    public int sendPreAppointmentNotifications(LocalDateTime now) {
        LocalDateTime until = now.plusHours(PRE_APPOINTMENT_HOURS);
        List<Appointment> appointments = appointmentGetService
                .findAllNeedingPreAppointmentNotification(now.toLocalDate(), until.toLocalDate()).stream()
                .filter(Appointment::isConfirmed)
                .filter(appointment -> !appointment.getConfirmedDateTime().isBefore(now)
                        && !appointment.getConfirmedDateTime().isAfter(until))
                .toList();

        appointments.forEach(appointment -> {
            List<Long> recipientIds = acceptedMemberIds(appointment,
                    participant -> participant.getAttendanceStatus() != AttendanceStatus.NOT_ATTENDING);
            appointmentEventPublisher.publishNotification(
                    appointmentEventFactory.appointmentSoon(appointment, recipientIds));
            appointment.markPreAppointmentNotified(now);
        });
        return appointments.size();
    }

    private List<Long> acceptedMemberIds(Appointment appointment, Predicate<AppointmentParticipant> condition) {
        return appointment.getParticipants().stream()
                .filter(participant -> participant.getStatus() == ParticipationStatus.ACCEPTED)
                .filter(condition)
                .map(participant -> participant.getMember().getId())
                .toList();
    }
}
