package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.MemberLocationRequest;
import com.jigmnnaga.application.dto.response.MemberLocationResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.MemberMapper;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.MemberLocation;
import com.jigmnnaga.domain.service.MemberGetService;
import com.jigmnnaga.domain.service.MemberLocationDeleteService;
import com.jigmnnaga.domain.service.MemberLocationGetService;
import com.jigmnnaga.domain.service.MemberLocationSaveService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MemberLocationUseCase {

    private static final int MAX_LOCATION_COUNT = 10;

    private final MemberGetService memberGetService;
    private final MemberLocationGetService memberLocationGetService;
    private final MemberLocationSaveService memberLocationSaveService;
    private final MemberLocationDeleteService memberLocationDeleteService;
    private final MemberMapper memberMapper;

    @Transactional(readOnly = true)
    public List<MemberLocationResponse> getLocations(Long memberId) {
        return memberLocationGetService.findAllByMemberId(memberId).stream()
                .map(memberMapper::toLocationResponse)
                .toList();
    }

    @Transactional
    public MemberLocationResponse createLocation(Long memberId, MemberLocationRequest request) {
        Member member = memberGetService.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (memberLocationGetService.countByMemberId(memberId) >= MAX_LOCATION_COUNT) {
            throw new BusinessException(ErrorCode.MEMBER_LOCATION_LIMIT_EXCEEDED);
        }

        MemberLocation location = memberLocationSaveService.save(memberMapper.toMemberLocation(member, request));
        return memberMapper.toLocationResponse(location);
    }

    @Transactional
    public MemberLocationResponse updateLocation(Long memberId, Long locationId, MemberLocationRequest request) {
        MemberLocation location = getMyLocation(memberId, locationId);
        location.update(request.name().strip(), request.address(), request.latitude(), request.longitude());
        return memberMapper.toLocationResponse(location);
    }

    @Transactional
    public void deleteLocation(Long memberId, Long locationId) {
        memberLocationDeleteService.delete(getMyLocation(memberId, locationId));
    }

    private MemberLocation getMyLocation(Long memberId, Long locationId) {
        return memberLocationGetService.findByIdAndMemberId(locationId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_LOCATION_NOT_FOUND));
    }
}
