package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.event.AppointmentEventFactory;
import com.jigmnnaga.application.event.AppointmentEventPublisher;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import com.jigmnnaga.domain.service.AppointmentDeleteService;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.domain.service.MemberGetService;
import com.jigmnnaga.domain.service.NotificationDeleteService;
import com.jigmnnaga.domain.service.RefreshTokenDeleteService;
import com.jigmnnaga.global.client.kakao.KakaoApiClient;
import com.jigmnnaga.global.util.TimeZones;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MemberWithdrawUseCase {

    private static final int MAX_MEMBER_COUNT_TO_DELETE_ON_HOST_WITHDRAWAL = 2;

    private final MemberGetService memberGetService;
    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final AppointmentDeleteService appointmentDeleteService;
    private final RefreshTokenDeleteService refreshTokenDeleteService;
    private final KakaoApiClient kakaoApiClient;
    private final NotificationDeleteService notificationDeleteService;
    private final AppointmentEventPublisher appointmentEventPublisher;
    private final AppointmentEventFactory appointmentEventFactory;

    @Transactional
    public void withdraw(Long memberId) {
        Member member = memberGetService.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(TimeZones.SEOUL);
        List<AppointmentParticipant> participations =
                appointmentParticipantGetService.findAllWithAppointmentByMemberId(memberId);

        kakaoApiClient.unlink(member.getKakaoId());

        participations.forEach(participation -> cleanUpParticipation(participation, now));
        refreshTokenDeleteService.deleteByMemberId(memberId);
        notificationDeleteService.deleteAllByMemberId(memberId);
        member.withdraw(now);
    }

    private void cleanUpParticipation(AppointmentParticipant participation, LocalDateTime now) {
        Appointment appointment = participation.getAppointment();

        if (participation.getStatus() != ParticipationStatus.ACCEPTED) {
            appointment.removeParticipant(participation);
            return;
        }
        if (!isActive(appointment, now)) {
            return;
        }
        if (participation.isHost()) {
            leaveHostedAppointment(appointment, participation);
            return;
        }
        appointment.removeParticipant(participation);
    }

    private void leaveHostedAppointment(Appointment appointment, AppointmentParticipant host) {
        if (appointment.getAcceptedMemberCount() <= MAX_MEMBER_COUNT_TO_DELETE_ON_HOST_WITHDRAWAL) {
            appointment.clearReferencesForDelete();
            appointmentDeleteService.delete(appointment);
            return;
        }

        AppointmentParticipant newHost = appointment.findEarliestAcceptedParticipantExcept(host).orElseThrow();
        appointment.delegateHost(host, newHost);
        appointmentEventPublisher.publishNotification(
                appointmentEventFactory.hostDelegated(appointment, newHost.getMember().getId()));
        appointment.removeParticipant(host);
    }

    private boolean isActive(Appointment appointment, LocalDateTime now) {
        return !appointment.isCanceled() && appointment.isUpcoming(now);
    }
}
