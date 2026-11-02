package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.response.InviteInfoResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.InviteMapper;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.service.AppointmentGetService;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.global.util.TimeZones;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InviteGetUseCase {

    private final AppointmentGetService appointmentGetService;
    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final InviteMapper inviteMapper;

    @Transactional(readOnly = true)
    public InviteInfoResponse getInvite(Long memberId, String inviteCode) {
        Appointment appointment = appointmentGetService.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_NOT_FOUND));

        List<AppointmentParticipant> participants =
                appointmentParticipantGetService.findAllWithMemberByAppointment(appointment);
        boolean expired = appointment.isInviteExpired(LocalDateTime.now(TimeZones.SEOUL));

        return inviteMapper.toInviteInfoResponse(appointment, participants, memberId, expired);
    }
}
