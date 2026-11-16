package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.GoogleLoginRequest;
import com.jigmnnaga.application.dto.response.LoginResponse;
import com.jigmnnaga.application.mapper.MemberMapper;
import com.jigmnnaga.domain.entity.AuthProvider;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.service.MemberGetService;
import com.jigmnnaga.domain.service.MemberSaveService;
import com.jigmnnaga.global.client.google.GoogleAuthClient;
import com.jigmnnaga.global.client.google.GoogleUserInfo;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GoogleLoginUseCase {

    private final GoogleAuthClient googleAuthClient;
    private final MemberGetService memberGetService;
    private final MemberSaveService memberSaveService;
    private final MemberMapper memberMapper;
    private final LoginTokenIssuer loginTokenIssuer;

    @Transactional
    public LoginResponse login(GoogleLoginRequest request) {
        GoogleUserInfo googleUserInfo = googleAuthClient.verify(request.idToken());

        Optional<Member> existingMember =
                memberGetService.findByProviderAndProviderId(AuthProvider.GOOGLE, googleUserInfo.subject());
        boolean isNewMember = existingMember.isEmpty();
        Member member = existingMember
                .orElseGet(() -> memberSaveService.save(memberMapper.toMember(googleUserInfo)));

        return loginTokenIssuer.issue(member, isNewMember);
    }
}
