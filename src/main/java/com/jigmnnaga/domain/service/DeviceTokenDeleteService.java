package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.repository.DeviceTokenRepository;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceTokenDeleteService {

    private final DeviceTokenRepository deviceTokenRepository;

    public int deleteByTokenAndMemberId(String token, Long memberId) {
        return deviceTokenRepository.deleteByTokenAndMemberId(token, memberId);
    }

    public void deleteAllByMemberId(Long memberId) {
        deviceTokenRepository.deleteAllByMemberId(memberId);
    }

    public void deleteAllByTokens(Collection<String> tokens) {
        if (!tokens.isEmpty()) {
            deviceTokenRepository.deleteAllByTokens(tokens);
        }
    }
}
