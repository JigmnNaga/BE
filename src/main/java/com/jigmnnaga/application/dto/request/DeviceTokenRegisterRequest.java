package com.jigmnnaga.application.dto.request;

import com.jigmnnaga.domain.entity.DevicePlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceTokenRegisterRequest(
        @Schema(description = "FCM 기기 토큰")
        @NotBlank @Size(max = 512) String token,

        @Schema(description = "기기 플랫폼 (ANDROID, IOS, WEB)", example = "ANDROID")
        DevicePlatform platform
) {
}
