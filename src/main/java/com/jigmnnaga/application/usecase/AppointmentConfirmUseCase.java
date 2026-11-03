package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.ConfirmDateTimeRequest;
import com.jigmnnaga.application.dto.request.ConfirmLocationRequest;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.validator.AppointmentAccessValidator;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentCandidateLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AppointmentConfirmUseCase {

    private final AppointmentAccessValidator appointmentAccessValidator;

    @Transactional
    public void confirmDateTime(Long memberId, Long appointmentId, ConfirmDateTimeRequest request) {
        Appointment appointment = appointmentAccessValidator.getHostAppointment(memberId, appointmentId);
        appointmentAccessValidator.validateNotCanceled(appointment);

        boolean isCandidateDate = appointment.getCandidateDates().stream()
                .anyMatch(candidateDate -> candidateDate.getCandidateDate().equals(request.date()));
        if (!isCandidateDate) {
            throw new BusinessException(ErrorCode.INVALID_CONFIRMED_DATE);
        }

        appointment.confirmDateTime(request.date(), request.time());
    }

    @Transactional
    public void confirmLocation(Long memberId, Long appointmentId, ConfirmLocationRequest request) {
        Appointment appointment = appointmentAccessValidator.getHostAppointment(memberId, appointmentId);
        appointmentAccessValidator.validateNotCanceled(appointment);

        AppointmentCandidateLocation location = appointment.getCandidateLocations().stream()
                .filter(candidateLocation -> candidateLocation.getId().equals(request.locationId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CONFIRMED_LOCATION));

        ConfirmLocationRequest.DetailPlaceRequest detailPlace = request.detailPlace();
        if (detailPlace == null) {
            appointment.confirmLocation(location, null, null, null, null);
            return;
        }
        appointment.confirmLocation(location, detailPlace.placeName(), detailPlace.address(),
                detailPlace.latitude(), detailPlace.longitude());
    }
}
