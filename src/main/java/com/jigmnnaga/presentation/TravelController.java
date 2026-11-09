package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.TravelModeSelectRequest;
import com.jigmnnaga.application.dto.response.TravelOptionsResponse;
import com.jigmnnaga.application.usecase.TravelOptionUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Travel", description = "약속 당일 이동수단 선택 관련 API")
@RestController
@RequestMapping("/api/v1/appointments/{appointmentId}/travel")
@RequiredArgsConstructor
public class TravelController {

    private final TravelOptionUseCase travelOptionUseCase;

    @Operation(summary = "이동수단 후보 조회", description = "현재 위치에서 확정된 약속 장소까지 버스·지하철·택시별 대표 경로(가장 빠른 경로)의 소요 시간, 구간 요약, 지도 경로, 지금 출발 시 도착 예정 시각을 조회합니다. 일부 수단의 경로를 조회하지 못하면 해당 수단만 제외됩니다. 요금은 제공하지 않습니다.")
    @GetMapping("/options")
    public CommonResponse<TravelOptionsResponse> getTravelOptions(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId,
            @Parameter(description = "현재 위치 위도", example = "37.4979") @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @Parameter(description = "현재 위치 경도", example = "127.0276") @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double longitude
    ) {
        TravelOptionsResponse response = travelOptionUseCase.getTravelOptions(memberId, appointmentId, latitude, longitude);
        return CommonResponse.success(ResponseMessage.TRAVEL_OPTIONS_GET_SUCCESS, response);
    }

    @Operation(summary = "이동수단 선택", description = "약속에 갈 이동수단과 그 예상 소요 시간을 저장합니다. 약속 시간 전까지 변경할 수 있습니다.")
    @PutMapping("/mode")
    public CommonResponse<Void> selectTravelMode(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId,
            @RequestBody @Valid TravelModeSelectRequest request
    ) {
        travelOptionUseCase.selectTravelMode(memberId, appointmentId, request);
        return CommonResponse.success(ResponseMessage.TRAVEL_MODE_SELECT_SUCCESS);
    }
}
