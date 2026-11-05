package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenDeleteService {

    private final RefreshTokenRepository refreshTokenRepository;

    public void deleteByMemberId(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }
}
