package com.jigmnnaga.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConfirmLocationRequest(
        @Schema(description = "확정 장소 ID (약속의 후보 장소 중 하나)", example = "1")
        @NotNull Long locationId,

        @Schema(description = "세부 위치 (나중에 정하는 경우 null)")
        @Valid DetailPlaceRequest detailPlace
) {

    public record DetailPlaceRequest(
            @Schema(description = "상호명", example = "스터디카페")
            @NotBlank String placeName,

            @Schema(description = "주소", example = "서울 강남구 강남대로 390")
            String address,

            @Schema(description = "위도", example = "37.4979")
            @NotNull Double latitude,

            @Schema(description = "경도", example = "127.0276")
            @NotNull Double longitude
    ) {
    }
}
