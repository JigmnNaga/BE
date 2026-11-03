package com.jigmnnaga.application.validator;

import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentAccessValidator {

    private final AppointmentParticipantGetService appointmentParticipantGetService;

    public Appointment getHostAppointment(Long memberId, Long appointmentId) {
        AppointmentParticipant participant = appointmentParticipantGetService
                .findAcceptedByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));
        if (!participant.isHost()) {
            throw new BusinessException(ErrorCode.NOT_APPOINTMENT_HOST);
        }
        return participant.getAppointment();
    }

    public void validateNotCanceled(Appointment appointment) {
        if (appointment.isCanceled()) {
            throw new BusinessException(ErrorCode.APPOINTMENT_CANCELED);
        }
    }
}
