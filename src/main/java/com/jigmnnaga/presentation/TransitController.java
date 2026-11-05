package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.TransitRouteRequest;
import com.jigmnnaga.application.dto.response.TransitRouteResponse;
import com.jigmnnaga.application.usecase.TransitRouteGetUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Transit", description = "대중교통 경로 조회 관련 API")
@RestController
@RequestMapping("/api/v1/transit")
@RequiredArgsConstructor
public class TransitController {

    private final TransitRouteGetUseCase transitRouteGetUseCase;

    @Operation(
            summary = "대중교통 경로 조회",
            description = "출발지와 도착지 좌표를 기반으로 대중교통 경로를 조회합니다. 버스, 지하철, 도보 구간 및 예상 소요 시간, 환승 횟수, 요금 정보를 반환합니다."
    )
    @GetMapping("/routes")
    public CommonResponse<TransitRouteResponse> getRoutes(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @Valid @ModelAttribute TransitRouteRequest request
    ) {
        TransitRouteResponse response = transitRouteGetUseCase.getRoutes(request);
        return CommonResponse.success(ResponseMessage.TRANSIT_ROUTE_SUCCESS, response);
    }
}