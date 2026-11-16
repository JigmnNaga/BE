package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.response.LoginResponse;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.service.RefreshTokenSaveService;
import com.jigmnnaga.global.jwt.JwtProvider;
import com.jigmnnaga.global.jwt.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginTokenIssuer {

    private final JwtProvider jwtProvider;
    private final TokenHasher tokenHasher;
    private final RefreshTokenSaveService refreshTokenSaveService;

    public LoginResponse issue(Member member, boolean isNewMember) {
        String accessToken = jwtProvider.createAccessToken(member.getId());
        String refreshToken = jwtProvider.createRefreshToken(member.getId());
        refreshTokenSaveService.saveOrRotate(member, tokenHasher.hash(refreshToken));

        return LoginResponse.builder()
                .memberId(member.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewMember(isNewMember)
                .build();
    }
}
