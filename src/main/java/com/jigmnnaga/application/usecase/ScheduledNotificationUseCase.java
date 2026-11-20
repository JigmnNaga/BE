package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.event.AppointmentEventFactory;
import com.jigmnnaga.application.event.AppointmentEventPublisher;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.AttendanceStatus;
import com.jigmnnaga.domain.entity.DepartureUpcomingStage;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import com.jigmnnaga.domain.service.AppointmentGetService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private static final long COMPLETION_DELAY_HOURS = 24;

    private static final int PERIODIC_AM_HOUR = 10;
    private static final int PERIODIC_PM_HOUR = 18;

    private static final long PREPARE_BEFORE_DEPARTURE_MINUTES = 30;

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

    @Transactional
    public int sendDepartureInputReminders(LocalDateTime now) {
        int count = 0;
        count += sendDepartureUpcomingReminders(now);
        count += sendDeparturePeriodicReminders(now);
        return count;
    }

    private int sendDepartureUpcomingReminders(LocalDateTime now) {
        LocalDate fromDate = now.toLocalDate();
        LocalDate toDate = now.plusHours(24).toLocalDate();

        List<Appointment> appointments = appointmentGetService
                .findAllConfirmedForDepartureInputReminder(fromDate, toDate).stream()
                .filter(Appointment::isConfirmed)
                .filter(appointment -> {
                    LocalDateTime confirmedAt = appointment.getConfirmedDateTime();
                    return !confirmedAt.isBefore(now) && !confirmedAt.isAfter(now.plusHours(24));
                })
                .toList();

        int count = 0;
        for (Appointment appointment : appointments) {
            LocalDateTime confirmedAt = appointment.getConfirmedDateTime();
            long minutesUntil = ChronoUnit.MINUTES.between(now, confirmedAt);
            DepartureUpcomingStage stage = DepartureUpcomingStage.from(minutesUntil).orElse(null);
            if (stage == null) {
                continue;
            }

            List<AppointmentParticipant> eligible = appointment.getParticipants().stream()
                    .filter(AppointmentParticipant::isEligibleForDepartureAutoNotif)
                    .filter(p -> !p.isDepartureNotifUpcomingSent(stage))
                    .toList();

            if (!eligible.isEmpty()) {
                List<Long> recipientIds = eligible.stream()
                        .map(p -> p.getMember().getId())
                        .toList();
                appointmentEventPublisher.publishNotification(
                        appointmentEventFactory.departureInputRequested(appointment, recipientIds));
                eligible.forEach(p -> p.markDepartureNotifUpcomingSent(stage, now));
                count++;
            }
        }
        return count;
    }

    private int sendDeparturePeriodicReminders(LocalDateTime now) {
        int hour = now.getHour();
        boolean isAmSlot = (hour == PERIODIC_AM_HOUR);
        boolean isPmSlot = (hour == PERIODIC_PM_HOUR);
        if (!isAmSlot && !isPmSlot) {
            return 0;
        }

        LocalDate fromDate = now.plusHours(24).toLocalDate();
        LocalDate toDate = now.plusDays(90).toLocalDate();

        List<Appointment> appointments = appointmentGetService
                .findAllConfirmedForDepartureInputReminder(fromDate, toDate).stream()
                .filter(Appointment::isConfirmed)
                .filter(appointment -> appointment.getConfirmedDateTime().isAfter(now.plusHours(24)))
                .toList();

        LocalDate today = now.toLocalDate();
        int count = 0;
        for (Appointment appointment : appointments) {
            List<AppointmentParticipant> eligible = appointment.getParticipants().stream()
                    .filter(AppointmentParticipant::isEligibleForDepartureAutoNotif)
                    .filter(p -> isAmSlot ? !p.isDepartureNotifPeriodicAmSentToday(today)
                                         : !p.isDepartureNotifPeriodicPmSentToday(today))
                    .filter(p -> p.isDeparturePeriodicGapSatisfied(now))
                    .toList();

            if (!eligible.isEmpty()) {
                List<Long> recipientIds = eligible.stream()
                        .map(p -> p.getMember().getId())
                        .toList();
                appointmentEventPublisher.publishNotification(
                        appointmentEventFactory.departureInputRequested(appointment, recipientIds));
                if (isAmSlot) {
                    eligible.forEach(p -> p.markDepartureNotifPeriodicAmSent(now));
                } else {
                    eligible.forEach(p -> p.markDepartureNotifPeriodicPmSent(now));
                }
                count++;
            }
        }
        return count;
    }

    /**
     * 출발 시간 알림을 발송한다.
     *
     * <p>출발 권장 시각 = 약속 확정 시각 - travelDurationMinutes
     *
     * <ul>
     *   <li>출발 준비 알림: [출발 권장 시각 - 30분, 출발 권장 시각) 구간. 소급 발송 없음.</li>
     *   <li>출발 알림: [출발 권장 시각, 약속 시각) 구간. 약속 시각 전이면 소급 발송.</li>
     * </ul>
     *
     * <p>두 알림의 발송 구간이 서로 겹치지 않으므로 동시 발송은 발생하지 않는다.
     */
    @Transactional
    public int sendDepartureTimeNotifications(LocalDateTime now) {
        LocalDate fromDate = now.toLocalDate();
        LocalDate toDate = now.plusDays(1).toLocalDate();

        List<Appointment> appointments = appointmentGetService
                .findAllConfirmedForDepartureInputReminder(fromDate, toDate).stream()
                .filter(Appointment::isConfirmed)
                .filter(appointment -> !appointment.getConfirmedDateTime().isBefore(now))
                .toList();

        int count = 0;
        for (Appointment appointment : appointments) {
            count += sendDepartureTimeNotificationsForAppointment(appointment, now);
        }
        return count;
    }

    private int sendDepartureTimeNotificationsForAppointment(Appointment appointment, LocalDateTime now) {
        LocalDateTime appointmentTime = appointment.getConfirmedDateTime();
        int count = 0;

        for (AppointmentParticipant participant : appointment.getParticipants()) {
            if (!participant.isEligibleForDepartureTimeNotif()) {
                continue;
            }

            LocalDateTime departureTime = appointmentTime.minusMinutes(participant.getTravelDurationMinutes());
            LocalDateTime prepareTime = departureTime.minusMinutes(PREPARE_BEFORE_DEPARTURE_MINUTES);

            // 출발 준비 알림: [prepareTime, departureTime)
            // 소급 발송 없음 — now >= departureTime 이면 창이 이미 닫힌 것으로 간주
            if (!participant.isDeparturePrepareNotifSent()
                    && !now.isBefore(prepareTime)
                    && now.isBefore(departureTime)) {
                appointmentEventPublisher.publishNotification(
                        appointmentEventFactory.departurePrepareReminder(
                                appointment, departureTime, List.of(participant.getMember().getId())));
                participant.markDeparturePrepareNotifSent(now);
                count++;
            }

            // 출발 알림: [departureTime, appointmentTime)
            // 소급 발송 허용 — now >= departureTime 이고 약속 시각 전이면 1회 발송
            if (!participant.isDepartureStartTimeNotifSent()
                    && !now.isBefore(departureTime)
                    && now.isBefore(appointmentTime)) {
                appointmentEventPublisher.publishNotification(
                        appointmentEventFactory.departureStartTimeReminder(
                                appointment, List.of(participant.getMember().getId())));
                participant.markDepartureStartTimeNotifSent(now);
                count++;
            }
        }
        return count;
    }

    @Transactional
    public int completeOverdueAppointments(LocalDateTime now) {
        List<Appointment> appointments = appointmentGetService.findAllNeedingCompletion(
                now.minusHours(COMPLETION_DELAY_HOURS));
        appointments.forEach(appointment -> appointment.markCompleted(now));
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
