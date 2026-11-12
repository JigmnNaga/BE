package com.jigmnnaga.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(

        @Schema(description = "변경할 닉네임 (1~20자, 생략하면 유지)", example = "태양")
        @Size(max = 20)
        @Pattern(regexp = "(?s).*\\S.*", message = "닉네임은 공백만 입력할 수 없습니다.")
        String nickname,

        @Schema(description = "업로드 URL 발급 시 받은 프로필 이미지 키 (생략하면 유지)", example = "profiles/1/3f2a9c1e-....jpg")
        @Size(max = 255)
        String profileImageKey
) {
}
