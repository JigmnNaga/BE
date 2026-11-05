package com.jigmnnaga.application.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record PlaceSearchResponse(
        List<PlaceResponse> places,
        boolean isEnd
) {

    @Builder
    public record PlaceResponse(
            String kakaoPlaceId,
            String placeName,
            String lineNames,
            String roadAddress,
            String jibunAddress,
            Double latitude,
            Double longitude
    ) {
    }
}
