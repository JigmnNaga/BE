package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.RefreshToken;
import com.jigmnnaga.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenSaveService {

    private final RefreshTokenRepository refreshTokenRepository;

    public void saveOrRotate(Member member, String tokenHash) {
        refreshTokenRepository.findByMemberId(member.getId())
                .ifPresentOrElse(
                        refreshToken -> refreshToken.rotate(tokenHash),
                        () -> refreshTokenRepository.save(RefreshToken.builder()
                                .member(member)
                                .tokenHash(tokenHash)
                                .build())
                );
    }
}
