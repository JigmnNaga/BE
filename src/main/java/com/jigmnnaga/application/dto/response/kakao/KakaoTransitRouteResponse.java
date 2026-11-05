package com.jigmnnaga.application.dto.response.kakao;

import java.util.List;

public record KakaoTransitRouteResponse(
        String status,
        SearchProperties properties,
        List<Route> routes
) {
    public record SearchProperties(
            Integer total,
            Integer bus,
            Integer subway,
            Integer busAndSubway,
            String landingURL
    ) {}

    public record Route(
            RouteProperties properties,
            List<Step> steps
    ) {}

    public record RouteProperties(
            String type,
            Integer totalDistance,
            Integer totalTime,
            Integer transfers
    ) {}

    public record Step(
            StepProperties properties,
            Path path
    ) {}

    public record StepProperties(
            String guidance,
            String type,
            Integer distance,
            Integer time,
            List<Stop> stops,
            List<Vehicle> vehicles
    ) {}

    public record Stop(
            String name
    ) {}

    public record Vehicle(
            String name,
            String type
    ) {}

    public record Path(
            List<List<Double>> points
    ) {}
}