package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.KakaoLoginRequest;
import com.jigmnnaga.application.dto.response.LoginResponse;
import com.jigmnnaga.application.usecase.KakaoLoginUseCase;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoLoginUseCase kakaoLoginUseCase;

    @Operation(summary = "카카오 로그인", description = "카카오 액세스 토큰으로 로그인 및 자동 회원가입을 처리하고 자체 JWT를 발급합니다.")
    @PostMapping("/kakao/login")
    public CommonResponse<LoginResponse> kakaoLogin(@RequestBody @Valid KakaoLoginRequest request) {
        LoginResponse response = kakaoLoginUseCase.login(request);
        return CommonResponse.success(ResponseMessage.LOGIN_SUCCESS, response);
    }
}
