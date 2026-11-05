package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.NotificationSettingRequest;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationSettingUseCase {

    private final AppointmentParticipantGetService appointmentParticipantGetService;

    @Transactional
    public void updateNotificationSetting(Long memberId, Long appointmentId, NotificationSettingRequest request) {
        appointmentParticipantGetService.findAcceptedByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT))
                .updateNotificationEnabled(request.enabled());
    }
}
