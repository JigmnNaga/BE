package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.KakaoLoginRequest;
import com.jigmnnaga.application.dto.response.LoginResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoUserInfoResponse;
import com.jigmnnaga.application.mapper.MemberMapper;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.service.MemberGetService;
import com.jigmnnaga.domain.service.MemberSaveService;
import com.jigmnnaga.global.client.kakao.KakaoApiClient;
import com.jigmnnaga.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class KakaoLoginUseCase {

    private final KakaoApiClient kakaoApiClient;
    private final MemberGetService memberGetService;
    private final MemberSaveService memberSaveService;
    private final MemberMapper memberMapper;
    private final JwtProvider jwtProvider;

    @Transactional
    public LoginResponse login(KakaoLoginRequest request) {
        KakaoUserInfoResponse kakaoUserInfoResponse = kakaoApiClient.getUserInfo(request.kakaoAccessToken());

        boolean isNewMember = memberGetService.findByKakaoId(kakaoUserInfoResponse.id()).isEmpty();
        Member member = memberGetService.findByKakaoId(kakaoUserInfoResponse.id())
                .orElseGet(() -> memberSaveService.save(memberMapper.toMember(kakaoUserInfoResponse)));

        String accessToken = jwtProvider.createAccessToken(member.getId());
        String refreshToken = jwtProvider.createRefreshToken(member.getId());

        return LoginResponse.builder()
                .memberId(member.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewMember(isNewMember)
                .build();
    }
}
