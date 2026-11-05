package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.KakaoLoginRequest;
import com.jigmnnaga.application.dto.request.TokenReissueRequest;
import com.jigmnnaga.application.dto.response.LoginResponse;
import com.jigmnnaga.application.dto.response.TokenResponse;
import com.jigmnnaga.application.usecase.KakaoLoginUseCase;
import com.jigmnnaga.application.usecase.LogoutUseCase;
import com.jigmnnaga.application.usecase.TokenReissueUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    private final TokenReissueUseCase tokenReissueUseCase;
    private final LogoutUseCase logoutUseCase;

    @Operation(summary = "카카오 로그인", description = "카카오 액세스 토큰으로 로그인 및 자동 회원가입을 처리하고 자체 JWT를 발급합니다.")
    @PostMapping("/kakao/login")
    public CommonResponse<LoginResponse> kakaoLogin(@RequestBody @Valid KakaoLoginRequest request) {
        LoginResponse response = kakaoLoginUseCase.login(request);
        return CommonResponse.success(ResponseMessage.LOGIN_SUCCESS, response);
    }

    @Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 액세스 토큰과 리프레시 토큰을 새로 발급합니다. 사용한 리프레시 토큰은 폐기되어 다시 사용할 수 없으므로, 응답으로 받은 새 리프레시 토큰을 저장해야 합니다.")
    @PostMapping("/reissue")
    public CommonResponse<TokenResponse> reissue(@RequestBody @Valid TokenReissueRequest request) {
        TokenResponse response = tokenReissueUseCase.reissue(request);
        return CommonResponse.success(ResponseMessage.TOKEN_REISSUE_SUCCESS, response);
    }

    @Operation(summary = "로그아웃", description = "저장된 리프레시 토큰을 삭제합니다. 이후 해당 리프레시 토큰으로 재발급할 수 없습니다.")
    @PostMapping("/logout")
    public CommonResponse<Void> logout(@Parameter(hidden = true) @LoginMember Long memberId) {
        logoutUseCase.logout(memberId);
        return CommonResponse.success(ResponseMessage.LOGOUT_SUCCESS);
    }
}
