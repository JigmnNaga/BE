package com.jigmnnaga.application.dto.response.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoPlaceSearchResponse(
        List<KakaoPlace> documents,
        KakaoMeta meta
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoPlace(
            String id,
            @JsonProperty("place_name") String placeName,
            @JsonProperty("category_name") String categoryName,
            @JsonProperty("address_name") String addressName,
            @JsonProperty("road_address_name") String roadAddressName,
            String x,
            String y
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoMeta(
            @JsonProperty("is_end") boolean isEnd
    ) {
    }
}
