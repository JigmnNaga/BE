package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import com.jigmnnaga.domain.service.AppointmentDeleteService;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.domain.service.MemberGetService;
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

    private final MemberGetService memberGetService;
    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final AppointmentDeleteService appointmentDeleteService;
    private final RefreshTokenDeleteService refreshTokenDeleteService;
    private final KakaoApiClient kakaoApiClient;

    @Transactional
    public void withdraw(Long memberId) {
        Member member = memberGetService.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(TimeZones.SEOUL);
        List<AppointmentParticipant> participations =
                appointmentParticipantGetService.findAllWithAppointmentByMemberId(memberId);

        validateNoHostedActiveAppointmentWithOthers(participations, now);
        kakaoApiClient.unlink(member.getKakaoId());

        participations.forEach(participation -> cleanUpParticipation(participation, now));
        refreshTokenDeleteService.deleteByMemberId(memberId);
        member.withdraw(now);
    }

    private void validateNoHostedActiveAppointmentWithOthers(List<AppointmentParticipant> participations,
                                                              LocalDateTime now) {
        boolean hasHostedActiveAppointmentWithOthers = participations.stream()
                .filter(AppointmentParticipant::isHost)
                .map(AppointmentParticipant::getAppointment)
                .anyMatch(appointment -> isActive(appointment, now) && appointment.getAcceptedMemberCount() > 1);
        if (hasHostedActiveAppointmentWithOthers) {
            throw new BusinessException(ErrorCode.WITHDRAWAL_BLOCKED_BY_HOSTED_APPOINTMENT);
        }
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
            appointment.clearReferencesForDelete();
            appointmentDeleteService.delete(appointment);
            return;
        }
        appointment.removeParticipant(participation);
    }

    private boolean isActive(Appointment appointment, LocalDateTime now) {
        return !appointment.isCanceled() && appointment.isUpcoming(now);
    }
}
