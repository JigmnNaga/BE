package com.jigmnnaga.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TokenReissueRequest(
        @Schema(description = "로그인 또는 이전 재발급 때 받은 리프레시 토큰")
        @NotBlank String refreshToken
) {
}
