package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.request.MemberLocationRequest;
import com.jigmnnaga.application.dto.response.MemberLocationResponse;
import com.jigmnnaga.application.dto.response.MyInfoResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoUserInfoResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.MemberLocation;
import com.jigmnnaga.domain.entity.Role;
import java.util.List;
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

    public MyInfoResponse toMyInfoResponse(Member member, List<MemberLocation> locations) {
        return MyInfoResponse.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .profileImageUrl(profileImageUrlResolver.resolve(member))
                .email(member.getEmail())
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
