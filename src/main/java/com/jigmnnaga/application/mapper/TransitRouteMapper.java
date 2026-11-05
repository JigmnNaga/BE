package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.response.TransitRouteResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoTransitRouteResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransitRouteMapper {

    public TransitRouteResponse toTransitRouteResponse(KakaoTransitRouteResponse response) {
        List<TransitRouteResponse.RouteResponse> routes = response.routes().stream()
                .map(this::toRouteResponse)
                .toList();

        return TransitRouteResponse.builder()
                .totalCount(routes.size())
                .routes(routes)
                .build();
    }

    private TransitRouteResponse.RouteResponse toRouteResponse(
            KakaoTransitRouteResponse.Route route
    ) {
        KakaoTransitRouteResponse.RouteProperties properties = route.properties();

        return TransitRouteResponse.RouteResponse.builder()
                .type(properties.type())
                .totalDistance(properties.totalDistance())
                .totalTime(properties.totalTime())
                .transfers(properties.transfers())
                .steps(route.steps().stream()
                        .map(this::toStepResponse)
                        .toList())
                .build();
    }

    private TransitRouteResponse.StepResponse toStepResponse(
            KakaoTransitRouteResponse.Step step
    ) {
        KakaoTransitRouteResponse.StepProperties properties = step.properties();

        return TransitRouteResponse.StepResponse.builder()
                .guidance(properties.guidance())
                .type(properties.type())
                .distance(properties.distance())
                .time(properties.time())
                .stops(toStopResponses(properties.stops()))
                .vehicles(toVehicleResponses(properties.vehicles()))
                .path(toCoordinateResponses(step.path()))
                .build();
    }

    private List<TransitRouteResponse.StopResponse> toStopResponses(
            List<KakaoTransitRouteResponse.Stop> stops
    ) {
        if (stops == null) {
            return List.of();
        }

        return stops.stream()
                .map(stop -> TransitRouteResponse.StopResponse.builder()
                        .name(stop.name())
                        .build())
                .toList();
    }

    private List<TransitRouteResponse.VehicleResponse> toVehicleResponses(
            List<KakaoTransitRouteResponse.Vehicle> vehicles
    ) {
        if (vehicles == null) {
            return List.of();
        }

        return vehicles.stream()
                .map(vehicle -> TransitRouteResponse.VehicleResponse.builder()
                        .name(vehicle.name())
                        .type(vehicle.type())
                        .build())
                .toList();
    }

    private List<TransitRouteResponse.CoordinateResponse> toCoordinateResponses(
            KakaoTransitRouteResponse.Path path
    ) {
        if (path == null || path.points() == null) {
            return List.of();
        }

        return path.points().stream()
                .filter(point -> point != null && point.size() >= 2)
                .map(point -> TransitRouteResponse.CoordinateResponse.builder()
                        .longitude(point.get(0))
                        .latitude(point.get(1))
                        .build())
                .toList();
    }
}