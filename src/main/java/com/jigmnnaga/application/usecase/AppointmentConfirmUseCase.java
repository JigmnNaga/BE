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
import com.jigmnnaga.global.cache.EtaCache;
import com.jigmnnaga.global.cache.LatenessCache;
import com.jigmnnaga.global.util.TimeZones;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AppointmentConfirmUseCase {

    private final AppointmentAccessValidator appointmentAccessValidator;
    private final AppointmentEventPublisher appointmentEventPublisher;
    private final AppointmentEventFactory appointmentEventFactory;
    private final EtaCache etaCache;
    private final LatenessCache latenessCache;

    @Transactional
    public void confirmDateTime(Long memberId, Long appointmentId, ConfirmDateTimeRequest request) {
        Appointment appointment = appointmentAccessValidator.getHostAppointment(memberId, appointmentId);
        appointmentAccessValidator.validateNotCanceled(appointment);

        boolean isCandidateDate = appointment.getCandidateDates().stream()
                .anyMatch(candidateDate -> candidateDate.getCandidateDate().equals(request.date()));
        if (!isCandidateDate) {
            throw new BusinessException(ErrorCode.INVALID_CONFIRMED_DATE);
        }

        // confirmDateTime() 호출 전에 변경 여부를 감지한다.
        // Objects.equals()로 null-safe 비교 (최초 확정 시 confirmedDate/Time이 null일 수 있음).
        boolean dateTimeChanging = !Objects.equals(request.date(), appointment.getConfirmedDate())
                || !Objects.equals(request.time(), appointment.getConfirmedTime());

        ConfirmationSnapshot before = ConfirmationSnapshot.of(appointment);
        appointment.confirmDateTime(request.date(), request.time());

        // 시각이 실제로 변경된 경우 ETA·지각 예측 Redis 캐시 무효화.
        // DB의 lateNotifSentAt은 Appointment.confirmDateTime() 내부에서 이미 초기화됨.
        // 무효화 후 다음 위치 업데이트 시 putIfNewer()가 반드시 true를 반환하여
        // 새 약속 시각 기준의 ETA 브로드캐스트·지각 예상 알림이 누락되지 않는다.
        if (dateTimeChanging) {
            etaCache.evictResultsByAppointment(appointmentId);
            latenessCache.evictByAppointment(appointmentId);
        }

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
