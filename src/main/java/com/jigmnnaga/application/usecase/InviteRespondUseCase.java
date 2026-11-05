package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.response.InviteAcceptResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.ParticipantRole;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import com.jigmnnaga.domain.service.AppointmentGetService;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.domain.service.AppointmentParticipantSaveService;
import com.jigmnnaga.domain.service.ChatMessageGetService;
import com.jigmnnaga.domain.service.MemberGetService;
import com.jigmnnaga.global.util.TimeZones;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InviteRespondUseCase {

    private final MemberGetService memberGetService;
    private final AppointmentGetService appointmentGetService;
    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final AppointmentParticipantSaveService appointmentParticipantSaveService;
    private final ChatMessageGetService chatMessageGetService;

    @Transactional
    public InviteAcceptResponse acceptInvite(Long memberId, String inviteCode) {
        AppointmentParticipant participant = getRespondableParticipant(memberId, inviteCode);
        participant.accept();
        chatMessageGetService.findLatestMessageId(participant.getAppointment().getId())
                .ifPresent(participant::readUpTo);
        appointmentParticipantSaveService.save(participant);

        return InviteAcceptResponse.builder()
                .appointmentId(participant.getAppointment().getId())
                .build();
    }

    @Transactional
    public void declineInvite(Long memberId, String inviteCode) {
        AppointmentParticipant participant = getRespondableParticipant(memberId, inviteCode);
        participant.decline();
        appointmentParticipantSaveService.save(participant);
    }

    private AppointmentParticipant getRespondableParticipant(Long memberId, String inviteCode) {
        Member member = memberGetService.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Appointment appointment = appointmentGetService.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_NOT_FOUND));

        if (appointment.isCanceled()) {
            throw new BusinessException(ErrorCode.APPOINTMENT_CANCELED);
        }
        if (appointment.isInviteExpired(LocalDateTime.now(TimeZones.SEOUL))) {
            throw new BusinessException(ErrorCode.INVITE_EXPIRED);
        }

        AppointmentParticipant participant = appointmentParticipantGetService
                .findByAppointmentAndMember(appointment, member)
                .orElseGet(() -> createPendingParticipant(appointment, member));

        if (participant.isKicked()) {
            throw new BusinessException(ErrorCode.KICKED_MEMBER_CANNOT_REJOIN);
        }
        if (participant.isHost()) {
            throw new BusinessException(ErrorCode.HOST_CANNOT_RESPOND);
        }
        if (participant.hasResponded()) {
            throw new BusinessException(ErrorCode.ALREADY_RESPONDED);
        }
        return participant;
    }

    private AppointmentParticipant createPendingParticipant(Appointment appointment, Member member) {
        AppointmentParticipant participant = AppointmentParticipant.builder()
                .member(member)
                .role(ParticipantRole.MEMBER)
                .status(ParticipationStatus.PENDING)
                .build();
        appointment.addParticipant(participant);
        return participant;
    }
}
