package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.request.MemberLocationRequest;
import com.jigmnnaga.application.dto.response.MemberLocationResponse;
import com.jigmnnaga.application.dto.response.MyInfoResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoUserInfoResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.AuthProvider;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.MemberLocation;
import com.jigmnnaga.domain.entity.Role;
import com.jigmnnaga.global.client.google.GoogleUserInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberMapper {

    private static final String DEFAULT_NICKNAME = "지금나가 사용자";

    private final ProfileImageUrlResolver profileImageUrlResolver;

    public Member toMember(KakaoUserInfoResponse kakaoUserInfoResponse) {
        KakaoUserInfoResponse.KakaoAccount kakaoAccount = kakaoUserInfoResponse.kakaoAccount();
        if (kakaoAccount == null || kakaoAccount.profile() == null || kakaoAccount.profile().nickname() == null) {
            throw new BusinessException(ErrorCode.KAKAO_ACCOUNT_INFO_MISSING);
        }
        KakaoUserInfoResponse.KakaoProfile profile = kakaoAccount.profile();

        return Member.builder()
                .provider(AuthProvider.KAKAO)
                .providerId(String.valueOf(kakaoUserInfoResponse.id()))
                .nickname(profile.nickname())
                .profileImageUrl(profile.profileImageUrl())
                .email(kakaoAccount.email())
                .role(Role.USER)
                .build();
    }

    public Member toMember(GoogleUserInfo googleUserInfo) {
        return Member.builder()
                .provider(AuthProvider.GOOGLE)
                .providerId(googleUserInfo.subject())
                .nickname(googleNickname(googleUserInfo))
                .profileImageUrl(googleUserInfo.pictureUrl())
                .email(googleUserInfo.email())
                .role(Role.USER)
                .build();
    }

    private String googleNickname(GoogleUserInfo googleUserInfo) {
        if (googleUserInfo.name() != null && !googleUserInfo.name().isBlank()) {
            return googleUserInfo.name().strip();
        }
        if (googleUserInfo.email() != null) {
            return googleUserInfo.email().substring(0, googleUserInfo.email().indexOf('@'));
        }
        return DEFAULT_NICKNAME;
    }

    public MyInfoResponse toMyInfoResponse(Member member, List<MemberLocation> locations) {
        return MyInfoResponse.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .profileImageUrl(profileImageUrlResolver.resolve(member))
                .profileColor(member.getProfileColor())
                .email(member.getEmail())
                .joinedAt(member.getCreatedAt() == null ? null : member.getCreatedAt().toLocalDate())
                .locations(locations.stream().map(this::toLocationResponse).toList())
                .build();
    }

    public MemberLocationResponse toLocationResponse(MemberLocation location) {
        return MemberLocationResponse.builder()
                .locationId(location.getId())
                .name(location.getName())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
    }

    public MemberLocation toMemberLocation(Member member, MemberLocationRequest request) {
        return MemberLocation.builder()
                .member(member)
                .name(request.name().strip())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build();
    }
}
