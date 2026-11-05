package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.RefreshToken;
import com.jigmnnaga.domain.repository.RefreshTokenRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenGetService {

    private final RefreshTokenRepository refreshTokenRepository;

    public Optional<RefreshToken> findByMemberId(Long memberId) {
        return refreshTokenRepository.findByMemberId(memberId);
    }
}
