package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.AppointmentLeaveRequest;
import com.jigmnnaga.application.dto.request.HostDelegateRequest;
import com.jigmnnaga.application.event.AppointmentEventFactory;
import com.jigmnnaga.application.event.AppointmentEventPublisher;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.validator.AppointmentAccessValidator;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ParticipantManageUseCase {

    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final AppointmentAccessValidator appointmentAccessValidator;
    private final AppointmentEventPublisher appointmentEventPublisher;
    private final AppointmentEventFactory appointmentEventFactory;

    @Transactional
    public void leaveAppointment(Long memberId, Long appointmentId, AppointmentLeaveRequest request) {
        AppointmentParticipant me = appointmentParticipantGetService
                .findAcceptedByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));
        Appointment appointment = me.getAppointment();

        if (me.isHost()) {
            if (appointment.getAcceptedMemberCount() == 1) {
                throw new BusinessException(ErrorCode.LAST_MEMBER_CANNOT_LEAVE);
            }
            if (request == null || request.newHostMemberId() == null) {
                throw new BusinessException(ErrorCode.NEW_HOST_REQUIRED);
            }
            AppointmentParticipant newHost = getOtherAcceptedParticipant(appointment, memberId, request.newHostMemberId());
            appointment.delegateHost(me, newHost);
            appointmentEventPublisher.publishNotification(
                    appointmentEventFactory.hostDelegated(appointment, newHost.getMember().getId()));
        }

        appointment.removeParticipant(me);
    }

    @Transactional
    public void kickParticipant(Long memberId, Long appointmentId, Long targetMemberId) {
        Appointment appointment = getManageableAppointment(memberId, appointmentId);
        AppointmentParticipant target = getOtherAcceptedParticipant(appointment, memberId, targetMemberId);
        target.kick();
        appointmentEventPublisher.publishNotification(appointmentEventFactory.kicked(appointment, targetMemberId));
    }

    @Transactional
    public void delegateHost(Long memberId, Long appointmentId, HostDelegateRequest request) {
        Appointment appointment = getManageableAppointment(memberId, appointmentId);
        AppointmentParticipant currentHost = appointment.findAcceptedParticipant(memberId).orElseThrow();
        AppointmentParticipant newHost = getOtherAcceptedParticipant(appointment, memberId, request.memberId());
        appointment.delegateHost(currentHost, newHost);
        appointmentEventPublisher.publishNotification(
                appointmentEventFactory.hostDelegated(appointment, newHost.getMember().getId()));
    }

    private Appointment getManageableAppointment(Long memberId, Long appointmentId) {
        Appointment appointment = appointmentAccessValidator.getHostAppointment(memberId, appointmentId);
        appointmentAccessValidator.validateNotCanceled(appointment);
        return appointment;
    }

    private AppointmentParticipant getOtherAcceptedParticipant(Appointment appointment, Long memberId,
                                                               Long targetMemberId) {
        if (memberId.equals(targetMemberId)) {
            throw new BusinessException(ErrorCode.INVALID_TARGET_MEMBER);
        }
        return appointment.findAcceptedParticipant(targetMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TARGET_MEMBER));
    }
}
