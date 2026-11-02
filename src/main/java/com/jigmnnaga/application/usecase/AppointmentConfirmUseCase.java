package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.ConfirmDateTimeRequest;
import com.jigmnnaga.application.dto.request.ConfirmLocationRequest;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentCandidateLocation;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AppointmentConfirmUseCase {

    private final AppointmentParticipantGetService appointmentParticipantGetService;

    @Transactional
    public void confirmDateTime(Long memberId, Long appointmentId, ConfirmDateTimeRequest request) {
        Appointment appointment = getHostAppointment(memberId, appointmentId);

        boolean isCandidateDate = appointment.getCandidateDates().stream()
                .anyMatch(candidateDate -> candidateDate.getCandidateDate().equals(request.date()));
        if (!isCandidateDate) {
            throw new BusinessException(ErrorCode.INVALID_CONFIRMED_DATE);
        }

        appointment.confirmDateTime(request.date(), request.time());
    }

    @Transactional
    public void confirmLocation(Long memberId, Long appointmentId, ConfirmLocationRequest request) {
        Appointment appointment = getHostAppointment(memberId, appointmentId);

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

    private Appointment getHostAppointment(Long memberId, Long appointmentId) {
        AppointmentParticipant participant = appointmentParticipantGetService
                .findAcceptedByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));
        if (!participant.isHost()) {
            throw new BusinessException(ErrorCode.NOT_APPOINTMENT_HOST);
        }
        return participant.getAppointment();
    }
}
