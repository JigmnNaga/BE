package com.jigmnnaga.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MemberLocationRequest(

        @Schema(description = "위치 이름 (예: 집, 회사, 학교)", example = "집")
        @NotBlank
        @Size(max = 255)
        String name,

        @Schema(description = "주소", example = "서울 강남구 테헤란로 152")
        @Size(max = 255)
        String address,

        @Schema(description = "위도", example = "37.4979420")
        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        BigDecimal latitude,

        @Schema(description = "경도", example = "127.0276210")
        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        BigDecimal longitude
) {
}
