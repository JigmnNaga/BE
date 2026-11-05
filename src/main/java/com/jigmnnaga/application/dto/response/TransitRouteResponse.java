package com.jigmnnaga.application.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record TransitRouteResponse(
        int totalCount,
        List<RouteResponse> routes
) {

    @Builder
    public record RouteResponse(
            String type,
            int totalDistance,
            int totalTime,
            int transfers,
            List<StepResponse> steps
    ) {}

    @Builder
    public record StepResponse(
            String guidance,
            String type,
            Integer distance,
            Integer time,
            List<StopResponse> stops,
            List<VehicleResponse> vehicles,
            List<CoordinateResponse> path
    ) {}

    @Builder
    public record StopResponse(
            String name
    ) {}

    @Builder
    public record VehicleResponse(
            String name,
            String type
    ) {}

    @Builder
    public record CoordinateResponse(
            double longitude,
            double latitude
    ) {}
}