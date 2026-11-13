package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.LocationUpdateRequest;
import com.jigmnnaga.application.dto.response.LocationBroadcastResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.DepartureStatus;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.global.cache.LocationCache;
import com.jigmnnaga.global.util.TimeZones;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LocationShareUseCase {

    private static final Duration THROTTLE_INTERVAL = Duration.ofSeconds(1);
    private static final String LOCATION_TOPIC_PREFIX = "/sub/appointments/";
    private static final String LOCATION_TOPIC_SUFFIX = "/location";

    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final LocationCache locationCache;
    private final EtaCalculateUseCase etaCalculateUseCase;
    private final ArrivalDetectUseCase arrivalDetectUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void updateLocation(Long memberId, Long appointmentId, LocationUpdateRequest request) {
        AppointmentParticipant me = appointmentParticipantGetService
                .findAcceptedByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));

        Appointment appointment = me.getAppointment();
        if (appointment.isCanceled()) {
            throw new BusinessException(ErrorCode.APPOINTMENT_CANCELED);
        }
        if (!appointment.isConfirmed()) {
            throw new BusinessException(ErrorCode.TRAVEL_DESTINATION_NOT_CONFIRMED);
        }
        if (me.getDepartureStatus() != DepartureStatus.DEPARTED) {
            throw new BusinessException(ErrorCode.NOT_DEPARTED);
        }

        LocalDateTime now = LocalDateTime.now(TimeZones.SEOUL);
        Optional<LocalDateTime> lastUpdated = locationCache.getLastUpdatedAt(appointmentId, memberId);
        if (lastUpdated.isPresent() && Duration.between(lastUpdated.get(), now).compareTo(THROTTLE_INTERVAL) < 0) {
            throw new BusinessException(ErrorCode.LOCATION_UPDATE_TOO_FREQUENT);
        }

        double latitude = request.latitude().doubleValue();
        double longitude = request.longitude().doubleValue();
        locationCache.put(appointmentId, memberId, latitude, longitude, me.getDepartureStatus(), now);

        // 1. 위치 먼저 브로드캐스트
        messagingTemplate.convertAndSend(LOCATION_TOPIC_PREFIX + appointmentId + LOCATION_TOPIC_SUFFIX,
                new LocationBroadcastResponse(memberId, latitude, longitude, me.getDepartureStatus(), now));

        // 2. 도착 감지 (반경 100m 이내 2회 연속 → ARRIVED로 변경, 커밋 후 /arrival 브로드캐스트)
        boolean arrived = arrivalDetectUseCase.detectAndArrive(me, appointmentId, latitude, longitude, now);

        // 3. 도착 전에만 ETA 계산 (쓰로틀 30초, 실패 시 무시)
        if (!arrived) {
            etaCalculateUseCase.calculateAndBroadcast(memberId, appointmentId, latitude, longitude,
                    me.getTravelMode(), appointment, now);
        }
    }
}
