package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.DeviceTokenRegisterRequest;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.DeviceToken;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.service.DeviceTokenDeleteService;
import com.jigmnnaga.domain.service.DeviceTokenGetService;
import com.jigmnnaga.domain.service.DeviceTokenSaveService;
import com.jigmnnaga.domain.service.MemberGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DeviceTokenUseCase {

    private final MemberGetService memberGetService;
    private final DeviceTokenGetService deviceTokenGetService;
    private final DeviceTokenSaveService deviceTokenSaveService;
    private final DeviceTokenDeleteService deviceTokenDeleteService;

    @Transactional
    public void registerToken(Long memberId, DeviceTokenRegisterRequest request) {
        Member member = memberGetService.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        String token = request.token().strip();

        deviceTokenGetService.findByToken(token).ifPresentOrElse(
                deviceToken -> deviceToken.reassign(member, request.platform()),
                () -> deviceTokenSaveService.save(DeviceToken.builder()
                        .member(member)
                        .token(token)
                        .platform(request.platform())
                        .build())
        );
    }

    @Transactional
    public void deleteToken(Long memberId, String token) {
        if (deviceTokenDeleteService.deleteByTokenAndMemberId(token.strip(), memberId) == 0) {
            throw new BusinessException(ErrorCode.DEVICE_TOKEN_NOT_FOUND);
        }
    }
}
