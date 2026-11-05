package com.jigmnnaga.application.dto.request;

import com.jigmnnaga.domain.entity.DateSelectionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AppointmentCreateRequest(
        @Schema(description = "약속 이름", example = "저녁 식사 모임")
        @NotBlank @Size(max = 30) String title,

        @Schema(description = "날짜 선택 방식 (SINGLE: 하루, RANGE: 기간, MULTIPLE: 날짜별)", example = "RANGE")
        @NotNull DateSelectionType dateSelectionType,

        @Schema(description = "후보 날짜 목록", example = "[\"2026-09-04\", \"2026-09-05\"]")
        @NotEmpty List<@NotNull LocalDate> candidateDates,

        @Schema(description = "후보 위치 목록 (최대 5개)")
        @NotEmpty @Size(max = 5) List<@Valid @NotNull CandidateLocationRequest> candidateLocations,

        @Schema(description = "참여 응답 기한", example = "2026-09-03T23:59:59")
        @NotNull LocalDateTime responseDeadline,

        @Schema(description = "이전 약속 멤버 바로 초대 (선택, 다시 약속 잡기 시 사용)")
        @Valid PreviousMembersRequest previousMembers
) {

    public record PreviousMembersRequest(
            @Schema(description = "이전 약속 ID", example = "1")
            @NotNull Long appointmentId,

            @Schema(description = "바로 초대할 이전 약속 멤버 ID 목록", example = "[2, 3]")
            @NotEmpty List<@NotNull Long> memberIds
    ) {
    }

    public record CandidateLocationRequest(
            @Schema(description = "장소 이름", example = "강남역")
            @NotBlank String placeName,

            @Schema(description = "주소", example = "서울특별시 강남구 역삼동")
            String address,

            @Schema(description = "위도", example = "37.497942")
            @NotNull Double latitude,

            @Schema(description = "경도", example = "127.027621")
            @NotNull Double longitude
    ) {
    }
}
