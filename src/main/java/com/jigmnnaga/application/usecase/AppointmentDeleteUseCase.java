package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.validator.AppointmentAccessValidator;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.service.AppointmentDeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AppointmentDeleteUseCase {

    private final AppointmentAccessValidator appointmentAccessValidator;
    private final AppointmentDeleteService appointmentDeleteService;

    @Transactional
    public void deleteAppointment(Long memberId, Long appointmentId) {
        Appointment appointment = appointmentAccessValidator.getHostAppointment(memberId, appointmentId);
        appointment.clearReferencesForDelete();
        appointmentDeleteService.delete(appointment);
    }
}
