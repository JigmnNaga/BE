package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.response.kakao.KakaoUserInfoResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.Role;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {

    public Member toMember(KakaoUserInfoResponse kakaoUserInfoResponse) {
        KakaoUserInfoResponse.KakaoAccount kakaoAccount = kakaoUserInfoResponse.kakaoAccount();
        if (kakaoAccount == null || kakaoAccount.profile() == null || kakaoAccount.profile().nickname() == null) {
            throw new BusinessException(ErrorCode.KAKAO_ACCOUNT_INFO_MISSING);
        }
        KakaoUserInfoResponse.KakaoProfile profile = kakaoAccount.profile();

        return Member.builder()
                .kakaoId(kakaoUserInfoResponse.id())
                .nickname(profile.nickname())
                .profileImageUrl(profile.profileImageUrl())
                .email(kakaoAccount.email())
                .role(Role.USER)
                .build();
    }
}
