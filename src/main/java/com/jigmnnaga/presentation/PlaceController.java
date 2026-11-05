package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.PlaceSearchType;
import com.jigmnnaga.application.dto.response.PlaceSearchResponse;
import com.jigmnnaga.application.usecase.PlaceSearchUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Place", description = "장소 검색 관련 API")
@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceSearchUseCase placeSearchUseCase;

    @Operation(summary = "장소 검색", description = "키워드로 장소를 검색합니다. STATION은 지하철역만 검색하며 같은 역의 노선을 한 결과로 묶어 lineNames로 반환합니다(후보 장소 선택). PLACE는 상호·장소 전체를 검색합니다(세부 위치 선택).")
    @GetMapping("/search")
    public CommonResponse<PlaceSearchResponse> searchPlaces(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @Parameter(description = "검색어", example = "강남") @RequestParam @NotBlank String query,
            @Parameter(description = "STATION: 지하철역, PLACE: 상호·장소", example = "STATION") @RequestParam PlaceSearchType type,
            @Parameter(description = "페이지 번호 (1~45)", example = "1") @RequestParam(defaultValue = "1") @Min(1) @Max(45) int page,
            @Parameter(description = "페이지 크기 (1~15)", example = "15") @RequestParam(defaultValue = "15") @Min(1) @Max(15) int size
    ) {
        PlaceSearchResponse response = placeSearchUseCase.searchPlaces(query, type, page, size);
        return CommonResponse.success(ResponseMessage.PLACE_SEARCH_SUCCESS, response);
    }
}
