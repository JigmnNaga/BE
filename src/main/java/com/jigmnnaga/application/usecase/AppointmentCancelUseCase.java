package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.event.AppointmentEventFactory;
import com.jigmnnaga.application.event.AppointmentEventPublisher;
import com.jigmnnaga.application.validator.AppointmentAccessValidator;
import com.jigmnnaga.domain.entity.Appointment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AppointmentCancelUseCase {

    private final AppointmentAccessValidator appointmentAccessValidator;
    private final AppointmentEventPublisher appointmentEventPublisher;
    private final AppointmentEventFactory appointmentEventFactory;

    @Transactional
    public void cancelAppointment(Long memberId, Long appointmentId) {
        Appointment appointment = appointmentAccessValidator.getHostAppointment(memberId, appointmentId);
        appointmentAccessValidator.validateNotCanceled(appointment);
        appointment.cancel();
        appointmentEventPublisher.sendSystemMessage(appointment, appointmentEventFactory.canceledChatMessage());
        appointmentEventPublisher.publishNotification(
                appointmentEventFactory.canceled(appointment, appointment.getAcceptedMemberIdsExcept(memberId)));
    }
}
