package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.LocationSharingUpdateRequest;
import com.jigmnnaga.application.dto.response.LocationSharingResponse;
import com.jigmnnaga.application.usecase.LocationSharingUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Location", description = "실시간 위치 공유 관련 API")
@RestController
@RequestMapping("/api/v1/appointments/{appointmentId}/location-sharing")
@RequiredArgsConstructor
public class LocationSharingController {

    private final LocationSharingUseCase locationSharingUseCase;

    @Operation(summary = "위치 공유 종료/다시 켜기", description = "enabled=false로 위치 공유를 종료합니다. 종료하면 저장된 최신 위치가 지워지고 /sub/appointments/{appointmentId}/location-end로 {memberId, reason: USER_REQUEST, endedAt}가 전송되며, 이후 보내는 위치는 거부됩니다. enabled=true로 다시 켤 수 있지만 도착한 뒤에는 켤 수 없습니다. 도착(자동 감지·도착하기)하면 별도 요청 없이 reason: ARRIVED로 공유가 종료됩니다.")
    @PatchMapping
    public CommonResponse<LocationSharingResponse> updateLocationSharing(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId,
            @RequestBody @Valid LocationSharingUpdateRequest request
    ) {
        LocationSharingResponse response = locationSharingUseCase.updateLocationSharing(memberId, appointmentId, request);
        return CommonResponse.success(ResponseMessage.LOCATION_SHARING_UPDATE_SUCCESS, response);
    }
}
