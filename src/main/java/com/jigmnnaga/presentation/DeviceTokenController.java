package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.DeviceTokenRegisterRequest;
import com.jigmnnaga.application.usecase.DeviceTokenUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Device", description = "푸시 알림 기기 토큰 관련 API")
@RestController
@RequestMapping("/api/v1/devices/token")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenUseCase deviceTokenUseCase;

    @Operation(summary = "기기 토큰 등록", description = "FCM 기기 토큰을 등록합니다. 앱 실행 시와 토큰이 갱신될 때 호출합니다. 같은 토큰이 다른 계정에 등록되어 있으면 현재 계정으로 옮깁니다.")
    @PutMapping
    public CommonResponse<Void> registerToken(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @RequestBody @Valid DeviceTokenRegisterRequest request
    ) {
        deviceTokenUseCase.registerToken(memberId, request);
        return CommonResponse.success(ResponseMessage.DEVICE_TOKEN_REGISTER_SUCCESS);
    }

    @Operation(summary = "기기 토큰 삭제", description = "이 기기에서 푸시 알림을 받지 않도록 기기 토큰을 삭제합니다. 로그아웃·탈퇴 시에는 서버에서 해당 회원의 기기 토큰을 모두 삭제합니다.")
    @DeleteMapping
    public CommonResponse<Void> deleteToken(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @Parameter(description = "삭제할 FCM 기기 토큰") @RequestParam @NotBlank String token
    ) {
        deviceTokenUseCase.deleteToken(memberId, token);
        return CommonResponse.success(ResponseMessage.DEVICE_TOKEN_DELETE_SUCCESS);
    }
}
