package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.ConfirmDateTimeRequest;
import com.jigmnnaga.application.dto.request.ConfirmLocationRequest;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.event.AppointmentEventFactory;
import com.jigmnnaga.application.event.AppointmentEventPublisher;
import com.jigmnnaga.application.validator.AppointmentAccessValidator;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentCandidateLocation;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.global.util.TimeZones;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AppointmentConfirmUseCase {

    private final AppointmentAccessValidator appointmentAccessValidator;
    private final AppointmentEventPublisher appointmentEventPublisher;
    private final AppointmentEventFactory appointmentEventFactory;

    @Transactional
    public void confirmDateTime(Long memberId, Long appointmentId, ConfirmDateTimeRequest request) {
        Appointment appointment = appointmentAccessValidator.getHostAppointment(memberId, appointmentId);
        appointmentAccessValidator.validateNotCanceled(appointment);

        boolean isCandidateDate = appointment.getCandidateDates().stream()
                .anyMatch(candidateDate -> candidateDate.getCandidateDate().equals(request.date()));
        if (!isCandidateDate) {
            throw new BusinessException(ErrorCode.INVALID_CONFIRMED_DATE);
        }

        ConfirmationSnapshot before = ConfirmationSnapshot.of(appointment);
        appointment.confirmDateTime(request.date(), request.time());
        publishConfirmationEvents(memberId, appointment, before);
    }

    @Transactional
    public void confirmLocation(Long memberId, Long appointmentId, ConfirmLocationRequest request) {
        Appointment appointment = appointmentAccessValidator.getHostAppointment(memberId, appointmentId);
        appointmentAccessValidator.validateNotCanceled(appointment);

        AppointmentCandidateLocation location = appointment.getCandidateLocations().stream()
                .filter(candidateLocation -> candidateLocation.getId().equals(request.locationId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CONFIRMED_LOCATION));

        ConfirmationSnapshot before = ConfirmationSnapshot.of(appointment);
        ConfirmLocationRequest.DetailPlaceRequest detailPlace = request.detailPlace();
        if (detailPlace == null) {
            appointment.confirmLocation(location, null, null, null, null);
        } else {
            appointment.confirmLocation(location, detailPlace.placeName(), detailPlace.address(),
                    detailPlace.latitude(), detailPlace.longitude());
        }
        publishConfirmationEvents(memberId, appointment, before);
    }

    private void publishConfirmationEvents(Long hostId, Appointment appointment, ConfirmationSnapshot before) {
        if (before.equals(ConfirmationSnapshot.of(appointment))) {
            return;
        }

        List<Long> recipientIds = appointment.getAcceptedMemberIdsExcept(hostId);
        if (!appointment.isConfirmed()) {
            if (before.confirmed()) {
                appointmentEventPublisher.publishNotification(appointmentEventFactory.infoChanged(
                        appointment, "확정된 장소가 변경돼 세부 장소를 다시 정하는 중이에요.", recipientIds));
            }
            return;
        }

        if (before.confirmed()) {
            appointmentEventPublisher.sendSystemMessage(appointment,
                    appointmentEventFactory.confirmedChangedChatMessage(appointment));
            appointmentEventPublisher.publishNotification(
                    appointmentEventFactory.confirmedChanged(appointment, recipientIds));
        } else {
            appointmentEventPublisher.sendSystemMessage(appointment,
                    appointmentEventFactory.confirmedChatMessage(appointment));
            appointmentEventPublisher.publishNotification(appointmentEventFactory.confirmed(appointment, recipientIds));
        }
        appointmentEventPublisher.publishNotification(
                appointmentEventFactory.attendanceRequested(appointment, recipientIds));

        sendDepartureInputRequestIfNeeded(appointment);
    }

    private void sendDepartureInputRequestIfNeeded(Appointment appointment) {
        if (!appointment.isConfirmed()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(TimeZones.SEOUL);
        LocalDateTime confirmedAt = appointment.getConfirmedDateTime();
        if (confirmedAt.isBefore(now)) {
            return;
        }
        List<AppointmentParticipant> eligible = appointment.getParticipants().stream()
                .filter(AppointmentParticipant::isEligibleForDepartureAutoNotif)
                .filter(p -> !p.isDepartureNotifInitialSent())
                .toList();
        if (!eligible.isEmpty()) {
            List<Long> recipientIds = eligible.stream()
                    .map(p -> p.getMember().getId())
                    .toList();
            appointmentEventPublisher.publishNotification(
                    appointmentEventFactory.departureInputRequested(appointment, recipientIds));
            eligible.forEach(p -> p.markDepartureNotifInitialSent(now));
        }
    }

    private record ConfirmationSnapshot(boolean confirmed, LocalDate date, LocalTime time, Long locationId,
                                        String detailPlaceName) {

        private static ConfirmationSnapshot of(Appointment appointment) {
            AppointmentCandidateLocation location = appointment.getConfirmedLocation();
            return new ConfirmationSnapshot(appointment.isConfirmed(), appointment.getConfirmedDate(),
                    appointment.getConfirmedTime(), location == null ? null : location.getId(),
                    appointment.getDetailPlaceName());
        }
    }
}
