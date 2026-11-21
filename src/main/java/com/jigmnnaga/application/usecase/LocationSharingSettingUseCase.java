package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.LocationSharingSettingRequest;
import com.jigmnnaga.application.dto.response.LocationSharingSettingResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.service.MemberGetService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LocationSharingSettingUseCase {

    private static final Set<Integer> ALLOWED_START_MINUTES = Set.of(10, 30, 60);

    private final MemberGetService memberGetService;

    @Transactional(readOnly = true)
    public LocationSharingSettingResponse getSetting(Long memberId) {
        return toResponse(findMember(memberId));
    }

    @Transactional
    public LocationSharingSettingResponse updateSetting(Long memberId, LocationSharingSettingRequest request) {
        if (!ALLOWED_START_MINUTES.contains(request.startMinutesBefore())) {
            throw new BusinessException(ErrorCode.INVALID_LOCATION_SHARING_START_MINUTES);
        }
        Member member = findMember(memberId);
        member.updateLocationSharingSetting(request.enabled(), request.scope(), request.startMinutesBefore());
        return toResponse(member);
    }

    private Member findMember(Long memberId) {
        return memberGetService.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private LocationSharingSettingResponse toResponse(Member member) {
        return LocationSharingSettingResponse.builder()
                .enabled(member.isLocationSharingEnabled())
                .scope(member.getLocationSharingScope())
                .startMinutesBefore(member.getLocationSharingStartMinutes())
                .build();
    }
}
