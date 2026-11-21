package com.jigmnnaga.application.dto.request;

import com.jigmnnaga.domain.entity.LocationSharingScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record LocationSharingSettingRequest(

        @Schema(description = "위치 공유 사용 여부", example = "true")
        @NotNull
        Boolean enabled,

        @Schema(description = "공유 범위 (PARTICIPANTS: 약속에 참여한 친구만, NONE: 공유 안 함)", example = "PARTICIPANTS")
        @NotNull
        LocationSharingScope scope,

        @Schema(description = "약속 몇 분 전부터 공유할지 (10, 30, 60 중 하나)", example = "30")
        @NotNull
        Integer startMinutesBefore
) {
}
