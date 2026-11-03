package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.AttendanceRespondRequest;
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

        participant.respondAttendance(request.attendanceStatus());
    }
}
