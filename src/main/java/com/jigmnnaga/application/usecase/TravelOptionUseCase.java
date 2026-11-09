package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.TransitRouteRequest;
import com.jigmnnaga.application.dto.request.TravelModeSelectRequest;
import com.jigmnnaga.application.dto.response.TransitRouteResponse;
import com.jigmnnaga.application.dto.response.TravelOptionsResponse;
import com.jigmnnaga.application.dto.response.TravelOptionsResponse.TravelOptionResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.TravelOptionMapper;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.TravelMode;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.global.client.kakao.KakaoMobilityApiClient;
import com.jigmnnaga.global.util.TimeZones;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TravelOptionUseCase {

    private static final String BUS_ROUTE_TYPE = "BUS";
    private static final String SUBWAY_ROUTE_TYPE = "SUBWAY";
    private static final String BUS_AND_SUBWAY_ROUTE_TYPE = "BUS_AND_SUBWAY";

    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final TransitRouteGetUseCase transitRouteGetUseCase;
    private final KakaoMobilityApiClient kakaoMobilityApiClient;
    private final TravelOptionMapper travelOptionMapper;

    @Transactional(readOnly = true)
    public TravelOptionsResponse getTravelOptions(Long memberId, Long appointmentId, double latitude, double longitude) {
        AppointmentParticipant me = getTravelableParticipant(memberId, appointmentId);
        Appointment appointment = me.getAppointment();
        LocalDateTime now = LocalDateTime.now(TimeZones.SEOUL);

        List<TravelOptionResponse> options = new ArrayList<>();
        List<TransitRouteResponse.RouteResponse> transitRoutes = findTransitRoutes(appointment, latitude, longitude);
        fastestRoute(transitRoutes, BUS_ROUTE_TYPE)
                .ifPresent(route -> options.add(travelOptionMapper.fromTransitRoute(TravelMode.BUS, route, now)));
        fastestRoute(transitRoutes, SUBWAY_ROUTE_TYPE)
                .or(() -> fastestRoute(transitRoutes, BUS_AND_SUBWAY_ROUTE_TYPE))
                .ifPresent(route -> options.add(travelOptionMapper.fromTransitRoute(TravelMode.SUBWAY, route, now)));
        kakaoMobilityApiClient.getCarRoute(longitude, latitude,
                        appointment.getDestinationLongitude(), appointment.getDestinationLatitude())
                .ifPresent(route -> options.add(travelOptionMapper.fromCarRoute(route, now)));

        return travelOptionMapper.toTravelOptionsResponse(appointment, me, options);
    }

    @Transactional
    public void selectTravelMode(Long memberId, Long appointmentId, TravelModeSelectRequest request) {
        AppointmentParticipant me = getTravelableParticipant(memberId, appointmentId);
        LocalDateTime now = LocalDateTime.now(TimeZones.SEOUL);
        if (me.getAppointment().isAppointmentTimePassed(now)) {
            throw new BusinessException(ErrorCode.TRAVEL_MODE_TIME_PASSED);
        }
        me.selectTravelMode(request.mode(), request.durationMinutes(), now);
    }

    private AppointmentParticipant getTravelableParticipant(Long memberId, Long appointmentId) {
        AppointmentParticipant participant = appointmentParticipantGetService
                .findAcceptedByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));
        Appointment appointment = participant.getAppointment();
        if (appointment.isCanceled()) {
            throw new BusinessException(ErrorCode.APPOINTMENT_CANCELED);
        }
        if (!appointment.isConfirmed()) {
            throw new BusinessException(ErrorCode.TRAVEL_DESTINATION_NOT_CONFIRMED);
        }
        return participant;
    }

    private List<TransitRouteResponse.RouteResponse> findTransitRoutes(Appointment appointment, double latitude,
                                                                      double longitude) {
        try {
            return transitRouteGetUseCase.getRoutes(new TransitRouteRequest(longitude, latitude,
                    appointment.getDestinationLongitude(), appointment.getDestinationLatitude())).routes();
        } catch (BusinessException e) {
            log.warn("대중교통 경로를 조회하지 못해 버스·지하철 후보를 제외합니다. reason={}", e.getErrorCode());
            return List.of();
        }
    }

    private Optional<TransitRouteResponse.RouteResponse> fastestRoute(List<TransitRouteResponse.RouteResponse> routes,
                                                                     String type) {
        return routes.stream()
                .filter(route -> type.equals(route.type()))
                .min(Comparator.comparingInt(TransitRouteResponse.RouteResponse::totalTime));
    }
}
