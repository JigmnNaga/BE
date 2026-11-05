package com.jigmnnaga.application.usecase;

import com.jigmnnaga.domain.service.DeviceTokenDeleteService;
import com.jigmnnaga.domain.service.RefreshTokenDeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LogoutUseCase {

    private final RefreshTokenDeleteService refreshTokenDeleteService;
    private final DeviceTokenDeleteService deviceTokenDeleteService;

    @Transactional
    public void logout(Long memberId) {
        refreshTokenDeleteService.deleteByMemberId(memberId);
        deviceTokenDeleteService.deleteAllByMemberId(memberId);
    }
}
