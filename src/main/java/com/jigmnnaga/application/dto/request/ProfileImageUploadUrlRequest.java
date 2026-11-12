package com.jigmnnaga.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProfileImageUploadUrlRequest(

        @Schema(description = "이미지 Content-Type (image/jpeg, image/png, image/webp)", example = "image/jpeg")
        @NotBlank
        String contentType,

        @Schema(description = "파일 크기(바이트), 최대 5MB", example = "204800")
        @NotNull
        @Positive
        @Max(5 * 1024 * 1024)
        Long contentLength
) {
}
