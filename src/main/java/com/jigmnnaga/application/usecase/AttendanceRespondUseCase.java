package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.AttendanceRespondRequest;
import com.jigmnnaga.application.event.AppointmentEventFactory;
import com.jigmnnaga.application.event.AppointmentEventPublisher;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.AttendanceStatus;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.global.util.TimeZones;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AttendanceRespondUseCase {

    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final AppointmentEventPublisher appointmentEventPublisher;
    private final AppointmentEventFactory appointmentEventFactory;

    @Transactional
    public void respondAttendance(Long memberId, Long appointmentId, AttendanceRespondRequest request) {
        if (request.attendanceStatus() == AttendanceStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ATTENDANCE_STATUS);
        }

        AppointmentParticipant participant = appointmentParticipantGetService
                .findAcceptedByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));
        Appointment appointment = participant.getAppointment();

        if (appointment.isCanceled()) {
            throw new BusinessException(ErrorCode.APPOINTMENT_CANCELED);
        }
        if (!appointment.isConfirmed()) {
            throw new BusinessException(ErrorCode.APPOINTMENT_NOT_CONFIRMED);
        }
        if (appointment.isAppointmentTimePassed(LocalDateTime.now(TimeZones.SEOUL))) {
            throw new BusinessException(ErrorCode.APPOINTMENT_TIME_PASSED);
        }

        AttendanceStatus previousStatus = participant.getAttendanceStatus();
        participant.respondAttendance(request.attendanceStatus());

        boolean isNewRescheduleRequest = request.attendanceStatus() == AttendanceStatus.RESCHEDULE_REQUESTED
                && previousStatus != AttendanceStatus.RESCHEDULE_REQUESTED;
        if (isNewRescheduleRequest) {
            notifyRescheduleRequested(appointment, participant);
        }
    }

    private void notifyRescheduleRequested(Appointment appointment, AppointmentParticipant requester) {
        String nickname = requester.getMember().getNickname();
        appointmentEventPublisher.sendSystemMessage(appointment,
                appointmentEventFactory.rescheduleRequestedChatMessage(nickname));
        if (!requester.isHost()) {
            appointment.getHostMemberId().ifPresent(hostId -> appointmentEventPublisher.publishNotification(
                    appointmentEventFactory.rescheduleRequested(appointment, nickname, hostId)));
        }
    }
}
