package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.LocationSharingSettingRequest;
import com.jigmnnaga.application.dto.request.MemberLocationRequest;
import com.jigmnnaga.application.dto.request.ProfileImageUploadUrlRequest;
import com.jigmnnaga.application.dto.request.ProfileUpdateRequest;
import com.jigmnnaga.application.dto.response.AppointmentStatsResponse;
import com.jigmnnaga.application.dto.response.LocationSharingSettingResponse;
import com.jigmnnaga.application.dto.response.MemberLocationResponse;
import com.jigmnnaga.application.dto.response.MyInfoResponse;
import com.jigmnnaga.application.dto.response.ProfileImageUploadUrlResponse;
import com.jigmnnaga.application.usecase.LocationSharingSettingUseCase;
import com.jigmnnaga.application.usecase.MemberGetUseCase;
import com.jigmnnaga.application.usecase.MemberLocationUseCase;
import com.jigmnnaga.application.usecase.MemberProfileUseCase;
import com.jigmnnaga.application.usecase.MemberWithdrawUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final MemberLocationUseCase memberLocationUseCase;
    private final MemberProfileUseCase memberProfileUseCase;
    private final MemberWithdrawUseCase memberWithdrawUseCase;
    private final LocationSharingSettingUseCase locationSharingSettingUseCase;

    @Operation(summary = "내 정보 조회", description = "닉네임, 프로필 이미지, 이메일, 기본 위치 목록을 조회합니다. 직접 올린 프로필 이미지는 1시간 동안 유효한 임시 URL로 내려가므로 URL을 저장하지 말고 필요할 때 다시 조회하세요. 기본 위치가 없으면 locations는 빈 목록입니다.")
    @GetMapping("/me")
    public CommonResponse<MyInfoResponse> getMyInfo(@Parameter(hidden = true) @LoginMember Long memberId) {
        MyInfoResponse response = memberGetUseCase.getMyInfo(memberId);
        return CommonResponse.success(ResponseMessage.MY_INFO_GET_SUCCESS, response);
    }

    @Operation(summary = "약속 통계 조회", description = "프로필 화면의 총 약속, 제때 도착, 지각 수와 제때 도착률(%)을 조회합니다. 확정된 뒤 약속 시간이 지난 약속 중 참석으로 응답한 약속만 집계하며, 취소된 약속은 제외합니다. 약속 시각(분 단위)까지 도착하면 제때 도착이고, 늦게 도착했거나 도착 기록이 없으면 지각입니다. 집계할 약속이 없으면 onTimeRate는 null입니다.")
    @GetMapping("/me/stats")
    public CommonResponse<AppointmentStatsResponse> getAppointmentStats(
            @Parameter(hidden = true) @LoginMember Long memberId
    ) {
        AppointmentStatsResponse response = memberGetUseCase.getAppointmentStats(memberId);
        return CommonResponse.success(ResponseMessage.APPOINTMENT_STATS_GET_SUCCESS, response);
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

    @Operation(summary = "기본 위치 목록 조회", description = "집, 회사, 학교 등 저장한 기본 위치 목록을 등록한 순서대로 조회합니다.")
    @GetMapping("/me/locations")
    public CommonResponse<List<MemberLocationResponse>> getLocations(
            @Parameter(hidden = true) @LoginMember Long memberId
    ) {
        List<MemberLocationResponse> response = memberLocationUseCase.getLocations(memberId);
        return CommonResponse.success(ResponseMessage.MEMBER_LOCATIONS_GET_SUCCESS, response);
    }

    @Operation(summary = "기본 위치 등록", description = "자주 사용하는 위치를 등록합니다. 장소 검색 결과에서 고른 장소명·주소·좌표를 보내면 됩니다. 최대 10개까지 저장할 수 있습니다.")
    @PostMapping("/me/locations")
    public CommonResponse<MemberLocationResponse> createLocation(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @RequestBody @Valid MemberLocationRequest request
    ) {
        MemberLocationResponse response = memberLocationUseCase.createLocation(memberId, request);
        return CommonResponse.success(ResponseMessage.MEMBER_LOCATION_CREATE_SUCCESS, response);
    }

    @Operation(summary = "기본 위치 수정", description = "저장한 위치의 이름, 주소, 좌표를 수정합니다. 모든 항목을 보내야 합니다.")
    @PutMapping("/me/locations/{locationId}")
    public CommonResponse<MemberLocationResponse> updateLocation(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long locationId,
            @RequestBody @Valid MemberLocationRequest request
    ) {
        MemberLocationResponse response = memberLocationUseCase.updateLocation(memberId, locationId, request);
        return CommonResponse.success(ResponseMessage.MEMBER_LOCATION_UPDATE_SUCCESS, response);
    }

    @Operation(summary = "기본 위치 삭제", description = "저장한 위치를 삭제합니다.")
    @DeleteMapping("/me/locations/{locationId}")
    public CommonResponse<Void> deleteLocation(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long locationId
    ) {
        memberLocationUseCase.deleteLocation(memberId, locationId);
        return CommonResponse.success(ResponseMessage.MEMBER_LOCATION_DELETE_SUCCESS);
    }

    @Operation(summary = "위치 공유 설정 조회", description = "마이페이지의 위치 공유 사용 여부, 공유 범위, 공유 시작 시간을 조회합니다. 한 번도 저장하지 않은 회원은 기본값(enabled=true, scope=PARTICIPANTS, startMinutesBefore=30)이 내려갑니다.")
    @GetMapping("/me/location-sharing")
    public CommonResponse<LocationSharingSettingResponse> getLocationSharingSetting(
            @Parameter(hidden = true) @LoginMember Long memberId
    ) {
        LocationSharingSettingResponse response = locationSharingSettingUseCase.getSetting(memberId);
        return CommonResponse.success(ResponseMessage.LOCATION_SHARING_SETTING_GET_SUCCESS, response);
    }

    @Operation(summary = "위치 공유 설정 저장", description = "위치 공유 사용 여부, 공유 범위(PARTICIPANTS: 약속에 참여한 친구만, NONE: 공유 안 함), 공유 시작 시간(약속 몇 분 전, 10·30·60 중 하나)을 저장합니다. 세 항목을 모두 보내야 하며, 저장된 설정을 그대로 돌려줍니다.")
    @PutMapping("/me/location-sharing")
    public CommonResponse<LocationSharingSettingResponse> updateLocationSharingSetting(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @RequestBody @Valid LocationSharingSettingRequest request
    ) {
        LocationSharingSettingResponse response = locationSharingSettingUseCase.updateSetting(memberId, request);
        return CommonResponse.success(ResponseMessage.LOCATION_SHARING_SETTING_UPDATE_SUCCESS, response);
    }

    @Operation(summary = "회원 탈퇴", description = "회원 정보를 익명화하고 카카오 연결을 끊습니다. 응답 대기 중인 초대와 다가오는 약속 참여는 삭제됩니다. 다가오는 약속의 방장인 경우 참여자가 2명 이하면 약속방을 삭제하고, 3명 이상이면 가장 먼저 참여한 멤버에게 방장을 위임합니다. 지난 약속·취소된 약속의 기록은 '탈퇴한 사용자'로 보존됩니다. 직접 올린 프로필 이미지와 기본 위치 목록도 삭제됩니다.")
    @DeleteMapping("/me")
    public CommonResponse<Void> withdraw(@Parameter(hidden = true) @LoginMember Long memberId) {
        memberWithdrawUseCase.withdraw(memberId);
        return CommonResponse.success(ResponseMessage.WITHDRAW_SUCCESS);
    }
}
