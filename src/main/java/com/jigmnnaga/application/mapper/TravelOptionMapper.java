package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.response.TransitRouteResponse;
import com.jigmnnaga.application.dto.response.TravelOptionsResponse;
import com.jigmnnaga.application.dto.response.TravelOptionsResponse.CoordinateResponse;
import com.jigmnnaga.application.dto.response.TravelOptionsResponse.TravelOptionResponse;
import com.jigmnnaga.application.dto.response.TravelOptionsResponse.TravelStepResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoCarRouteResponse;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.TravelMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TravelOptionMapper {

    private static final int SECONDS_PER_MINUTE = 60;
    private static final String TAXI_STEP_TYPE = "TAXI";
    private static final String VEHICLE_NAME_SEPARATOR = ", ";

    public TravelOptionsResponse toTravelOptionsResponse(Appointment appointment, AppointmentParticipant me,
                                                         List<TravelOptionResponse> options) {
        return TravelOptionsResponse.builder()
                .destinationName(appointment.getDestinationName())
                .destinationLatitude(appointment.getDestinationLatitude())
                .destinationLongitude(appointment.getDestinationLongitude())
                .appointmentDateTime(appointment.getConfirmedDateTime())
                .myTravelMode(me.getTravelMode())
                .options(options)
                .build();
    }

    public TravelOptionResponse fromTransitRoute(TravelMode mode, TransitRouteResponse.RouteResponse route,
                                                 LocalDateTime now) {
        int durationMinutes = toMinutes(route.totalTime());
        List<CoordinateResponse> path = new ArrayList<>();
        route.steps().forEach(step -> step.path().forEach(point -> path.add(CoordinateResponse.builder()
                .latitude(point.latitude())
                .longitude(point.longitude())
                .build())));

        return TravelOptionResponse.builder()
                .mode(mode)
                .durationMinutes(durationMinutes)
                .distanceMeters(route.totalDistance())
                .estimatedArrivalTime(now.plusMinutes(durationMinutes))
                .steps(route.steps().stream()
                        .map(step -> TravelStepResponse.builder()
                                .type(step.type())
                                .minutes(toMinutes(Objects.requireNonNullElse(step.time(), 0)))
                                .name(vehicleNames(step))
                                .build())
                        .toList())
                .path(path)
                .build();
    }

    public TravelOptionResponse fromCarRoute(KakaoCarRouteResponse.Route route, LocalDateTime now) {
        int durationMinutes = toMinutes(route.summary().duration());
        List<CoordinateResponse> path = new ArrayList<>();
        if (route.sections() != null) {
            route.sections().forEach(section -> section.roads().forEach(road -> {
                List<Double> vertexes = road.vertexes();
                for (int i = 0; i + 1 < vertexes.size(); i += 2) {
                    path.add(CoordinateResponse.builder()
                            .longitude(vertexes.get(i))
                            .latitude(vertexes.get(i + 1))
                            .build());
                }
            }));
        }

        return TravelOptionResponse.builder()
                .mode(TravelMode.TAXI)
                .durationMinutes(durationMinutes)
                .distanceMeters(Objects.requireNonNullElse(route.summary().distance(), 0))
                .estimatedArrivalTime(now.plusMinutes(durationMinutes))
                .steps(List.of(TravelStepResponse.builder()
                        .type(TAXI_STEP_TYPE)
                        .minutes(durationMinutes)
                        .build()))
                .path(path)
                .build();
    }

    private String vehicleNames(TransitRouteResponse.StepResponse step) {
        if (step.vehicles() == null || step.vehicles().isEmpty()) {
            return null;
        }
        return String.join(VEHICLE_NAME_SEPARATOR, step.vehicles().stream()
                .map(TransitRouteResponse.VehicleResponse::name)
                .toList());
    }

    private int toMinutes(int seconds) {
        return (seconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE;
    }
}
