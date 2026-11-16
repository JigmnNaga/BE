package com.jigmnnaga.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @Schema(description = "안드로이드 구글 로그인으로 받은 ID 토큰 (serverClientId에 서버의 웹 클라이언트 ID 지정)")
        @NotBlank String idToken
) {
}
