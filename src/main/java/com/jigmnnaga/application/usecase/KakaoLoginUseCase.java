package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.KakaoLoginRequest;
import com.jigmnnaga.application.dto.response.LoginResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoUserInfoResponse;
import com.jigmnnaga.application.mapper.MemberMapper;
import com.jigmnnaga.domain.entity.AuthProvider;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.service.MemberGetService;
import com.jigmnnaga.domain.service.MemberSaveService;
import com.jigmnnaga.global.client.kakao.KakaoApiClient;
import java.util.Optional;
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
    private final LoginTokenIssuer loginTokenIssuer;

    @Transactional
    public LoginResponse login(KakaoLoginRequest request) {
        KakaoUserInfoResponse kakaoUserInfoResponse = kakaoApiClient.getUserInfo(request.kakaoAccessToken());

        Optional<Member> existingMember = memberGetService.findByProviderAndProviderId(
                AuthProvider.KAKAO, String.valueOf(kakaoUserInfoResponse.id()));
        boolean isNewMember = existingMember.isEmpty();
        existingMember.ifPresent(member -> member.fillEmailIfAbsent(kakaoEmail(kakaoUserInfoResponse)));
        Member member = existingMember
                .orElseGet(() -> memberSaveService.save(memberMapper.toMember(kakaoUserInfoResponse)));

        return loginTokenIssuer.issue(member, isNewMember);
    }

    private String kakaoEmail(KakaoUserInfoResponse kakaoUserInfoResponse) {
        return kakaoUserInfoResponse.kakaoAccount() == null ? null : kakaoUserInfoResponse.kakaoAccount().email();
    }
}
