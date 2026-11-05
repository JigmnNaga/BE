package com.jigmnnaga.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record AppointmentLeaveRequest(
        @Schema(description = "새 방장 회원 ID (방장이 나갈 때만 필수)", example = "2")
        Long newHostMemberId
) {
}
