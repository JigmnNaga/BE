package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.LocationSharingUpdateRequest;
import com.jigmnnaga.application.dto.response.LocationSharingEndedResponse.Reason;
import com.jigmnnaga.application.dto.response.LocationSharingResponse;
import com.jigmnnaga.application.event.LocationSharingEndPublisher;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.global.util.TimeZones;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LocationSharingUseCase {

    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final LocationSharingEndPublisher locationSharingEndPublisher;

    @Transactional
    public LocationSharingResponse updateLocationSharing(Long memberId, Long appointmentId,
                                                         LocationSharingUpdateRequest request) {
        AppointmentParticipant me = appointmentParticipantGetService
                .findAcceptedByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));

        if (request.enabled()) {
            me.resumeLocationSharing();
        } else if (me.isLocationSharingEnabled()) {
            me.stopLocationSharing();
            locationSharingEndPublisher.publishAfterCommit(appointmentId, memberId, Reason.USER_REQUEST,
                    LocalDateTime.now(TimeZones.SEOUL));
        }

        return LocationSharingResponse.builder()
                .enabled(me.isLocationSharingEnabled())
                .build();
    }
}
