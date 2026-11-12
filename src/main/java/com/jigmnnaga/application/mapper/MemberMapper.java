package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.response.MyInfoResponse;
import com.jigmnnaga.application.dto.response.MyInfoResponse.DefaultLocationResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoUserInfoResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberMapper {

    private final ProfileImageUrlResolver profileImageUrlResolver;

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

    public MyInfoResponse toMyInfoResponse(Member member) {
        return MyInfoResponse.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .profileImageUrl(profileImageUrlResolver.resolve(member))
                .email(member.getEmail())
                .defaultLocation(member.hasDefaultLocation() ? toDefaultLocationResponse(member) : null)
                .build();
    }

    private DefaultLocationResponse toDefaultLocationResponse(Member member) {
        return DefaultLocationResponse.builder()
                .name(member.getDefaultLocationName())
                .address(member.getDefaultLocationAddress())
                .latitude(member.getDefaultLatitude())
                .longitude(member.getDefaultLongitude())
                .build();
    }
}
