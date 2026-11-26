package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.NotificationSettingsRequest;
import com.jigmnnaga.application.dto.response.NotificationSettingsResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.service.MemberGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationSettingsUseCase {

    private final MemberGetService memberGetService;

    @Transactional(readOnly = true)
    public NotificationSettingsResponse getSettings(Long memberId) {
        return toResponse(findMember(memberId));
    }

    @Transactional
    public NotificationSettingsResponse updateSettings(Long memberId, NotificationSettingsRequest request) {
        Member member = findMember(memberId);
        member.updateNotificationSettings(
                request.allNotificationsEnabled(),
                request.appointmentReminderEnabled(),
                request.departureTimeEnabled(),
                request.friendArrivalEnabled(),
                request.marketingEnabled());
        return toResponse(member);
    }

    private Member findMember(Long memberId) {
        return memberGetService.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private NotificationSettingsResponse toResponse(Member member) {
        return NotificationSettingsResponse.builder()
                .allNotificationsEnabled(member.isAllNotificationsEnabled())
                .appointmentReminderEnabled(member.isAppointmentReminderEnabled())
                .departureTimeEnabled(member.isDepartureTimeEnabled())
                .friendArrivalEnabled(member.isFriendArrivalEnabled())
                .marketingEnabled(member.isMarketingEnabled())
                .build();
    }
}
