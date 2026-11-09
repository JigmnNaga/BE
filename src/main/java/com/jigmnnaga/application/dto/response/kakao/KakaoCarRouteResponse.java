package com.jigmnnaga.application.dto.response.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoCarRouteResponse(
        List<Route> routes
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(
            @JsonProperty("result_code") Integer resultCode,
            Summary summary,
            List<Section> sections
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(
            Integer distance,
            Integer duration
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Section(
            List<Road> roads
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Road(
            List<Double> vertexes
    ) {
    }
}
