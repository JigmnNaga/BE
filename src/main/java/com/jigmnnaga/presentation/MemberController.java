package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.DefaultLocationRequest;
import com.jigmnnaga.application.dto.request.ProfileImageUploadUrlRequest;
import com.jigmnnaga.application.dto.request.ProfileUpdateRequest;
import com.jigmnnaga.application.dto.response.MyInfoResponse;
import com.jigmnnaga.application.dto.response.ProfileImageUploadUrlResponse;
import com.jigmnnaga.application.usecase.MemberGetUseCase;
import com.jigmnnaga.application.usecase.MemberProfileUseCase;
import com.jigmnnaga.application.usecase.MemberWithdrawUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "회원 관련 API")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberGetUseCase memberGetUseCase;
    private final MemberProfileUseCase memberProfileUseCase;
    private final MemberWithdrawUseCase memberWithdrawUseCase;

    @Operation(summary = "내 정보 조회", description = "닉네임, 프로필 이미지, 이메일, 기본 위치를 조회합니다. 직접 올린 프로필 이미지는 1시간 동안 유효한 임시 URL로 내려가므로 URL을 저장하지 말고 필요할 때 다시 조회하세요. 기본 위치가 없으면 defaultLocation은 null입니다.")
    @GetMapping("/me")
    public CommonResponse<MyInfoResponse> getMyInfo(@Parameter(hidden = true) @LoginMember Long memberId) {
        MyInfoResponse response = memberGetUseCase.getMyInfo(memberId);
        return CommonResponse.success(ResponseMessage.MY_INFO_GET_SUCCESS, response);
    }

    @Operation(summary = "프로필 이미지 업로드 URL 발급", description = "S3에 프로필 이미지를 직접 올릴 수 있는 업로드 URL(10분 유효)을 발급합니다. 발급받은 uploadUrl로 PUT 요청을 보내되, 요청 헤더의 Content-Type과 Content-Length를 발급 시 보낸 값과 똑같이 맞춰야 합니다. 업로드가 끝나면 profileImageKey로 프로필 수정 API를 호출하세요. JPG, PNG, WEBP만 가능하며 최대 5MB입니다.")
    @PostMapping("/me/profile-image/upload-url")
    public CommonResponse<ProfileImageUploadUrlResponse> issueProfileImageUploadUrl(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @RequestBody @Valid ProfileImageUploadUrlRequest request
    ) {
        ProfileImageUploadUrlResponse response = memberProfileUseCase.issueUploadUrl(memberId, request);
        return CommonResponse.success(ResponseMessage.PROFILE_IMAGE_UPLOAD_URL_ISSUE_SUCCESS, response);
    }

    @Operation(summary = "프로필 수정", description = "닉네임(1~20자)과 프로필 이미지를 수정합니다. 보낸 항목만 변경되며, 둘 다 생략할 수 없습니다. profileImageKey는 업로드 URL 발급 시 받은 키를 S3 업로드 완료 후 보내야 하며, 기존에 올린 이미지는 삭제됩니다.")
    @PatchMapping("/me/profile")
    public CommonResponse<MyInfoResponse> updateProfile(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @RequestBody @Valid ProfileUpdateRequest request
    ) {
        MyInfoResponse response = memberProfileUseCase.updateProfile(memberId, request);
        return CommonResponse.success(ResponseMessage.PROFILE_UPDATE_SUCCESS, response);
    }

    @Operation(summary = "프로필 이미지 삭제", description = "프로필 이미지를 기본 이미지로 변경합니다. 이후 profileImageUrl은 null로 내려갑니다.")
    @DeleteMapping("/me/profile-image")
    public CommonResponse<Void> deleteProfileImage(@Parameter(hidden = true) @LoginMember Long memberId) {
        memberProfileUseCase.deleteProfileImage(memberId);
        return CommonResponse.success(ResponseMessage.PROFILE_IMAGE_DELETE_SUCCESS);
    }

    @Operation(summary = "기본 위치 등록/수정", description = "자주 출발하는 장소 1곳을 기본 위치로 저장합니다. 이미 있으면 덮어씁니다.")
    @PutMapping("/me/default-location")
    public CommonResponse<MyInfoResponse> updateDefaultLocation(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @RequestBody @Valid DefaultLocationRequest request
    ) {
        MyInfoResponse response = memberProfileUseCase.updateDefaultLocation(memberId, request);
        return CommonResponse.success(ResponseMessage.DEFAULT_LOCATION_UPDATE_SUCCESS, response);
    }

    @Operation(summary = "기본 위치 삭제", description = "저장한 기본 위치를 삭제합니다.")
    @DeleteMapping("/me/default-location")
    public CommonResponse<Void> deleteDefaultLocation(@Parameter(hidden = true) @LoginMember Long memberId) {
        memberProfileUseCase.deleteDefaultLocation(memberId);
        return CommonResponse.success(ResponseMessage.DEFAULT_LOCATION_DELETE_SUCCESS);
    }

    @Operation(summary = "회원 탈퇴", description = "회원 정보를 익명화하고 카카오 연결을 끊습니다. 응답 대기 중인 초대와 다가오는 약속 참여는 삭제됩니다. 다가오는 약속의 방장인 경우 참여자가 2명 이하면 약속방을 삭제하고, 3명 이상이면 가장 먼저 참여한 멤버에게 방장을 위임합니다. 지난 약속·취소된 약속의 기록은 '탈퇴한 사용자'로 보존됩니다. 직접 올린 프로필 이미지와 기본 위치도 삭제됩니다.")
    @DeleteMapping("/me")
    public CommonResponse<Void> withdraw(@Parameter(hidden = true) @LoginMember Long memberId) {
        memberWithdrawUseCase.withdraw(memberId);
        return CommonResponse.success(ResponseMessage.WITHDRAW_SUCCESS);
    }
}
