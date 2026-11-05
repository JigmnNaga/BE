package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.TokenReissueRequest;
import com.jigmnnaga.application.dto.response.TokenResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.RefreshToken;
import com.jigmnnaga.domain.service.RefreshTokenGetService;
import com.jigmnnaga.global.jwt.JwtProvider;
import com.jigmnnaga.global.jwt.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TokenReissueUseCase {

    private final JwtProvider jwtProvider;
    private final TokenHasher tokenHasher;
    private final RefreshTokenGetService refreshTokenGetService;

    @Transactional
    public TokenResponse reissue(TokenReissueRequest request) {
        Long memberId = jwtProvider.getMemberIdFromRefreshToken(request.refreshToken());
        RefreshToken storedRefreshToken = refreshTokenGetService.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (!storedRefreshToken.matches(tokenHasher.hash(request.refreshToken()))) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String accessToken = jwtProvider.createAccessToken(memberId);
        String refreshToken = jwtProvider.createRefreshToken(memberId);
        storedRefreshToken.rotate(tokenHasher.hash(refreshToken));

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
