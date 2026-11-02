package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.response.AppointmentDetailResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.AppointmentMapper;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.global.config.InviteProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AppointmentDetailGetUseCase {

    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final AppointmentMapper appointmentMapper;
    private final InviteProperties inviteProperties;

    @Transactional(readOnly = true)
    public AppointmentDetailResponse getAppointmentDetail(Long memberId, Long appointmentId) {
        AppointmentParticipant me = appointmentParticipantGetService
                .findAcceptedByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));
        Appointment appointment = me.getAppointment();

        List<AppointmentParticipant> participants =
                appointmentParticipantGetService.findAllAcceptedWithMemberByAppointmentId(appointmentId);
        String inviteUrl = inviteProperties.inviteUrl(appointment.getInviteCode());

        return appointmentMapper.toDetailResponse(appointment, participants, me, inviteUrl);
    }
}
