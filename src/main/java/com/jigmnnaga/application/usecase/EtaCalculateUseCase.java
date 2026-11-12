package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.TransitRouteRequest;
import com.jigmnnaga.application.dto.response.EtaBroadcastResponse;
import com.jigmnnaga.application.dto.response.TransitRouteResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.TravelMode;
import com.jigmnnaga.global.cache.EtaCache;
import com.jigmnnaga.global.client.kakao.KakaoMobilityApiClient;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EtaCalculateUseCase {

    private static final String BUS_ROUTE_TYPE = "BUS";
    private static final String SUBWAY_ROUTE_TYPE = "SUBWAY";
    private static final String BUS_AND_SUBWAY_ROUTE_TYPE = "BUS_AND_SUBWAY";
    private static final String ETA_TOPIC_PREFIX = "/sub/appointments/";
    private static final String ETA_TOPIC_SUFFIX = "/eta";
    private static final int SECONDS_PER_MINUTE = 60;

    private final TransitRouteGetUseCase transitRouteGetUseCase;
    private final KakaoMobilityApiClient kakaoMobilityApiClient;
    private final EtaCache etaCache;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 현재 위치를 기반으로 ETA를 계산하고, 갱신된 경우 ACCEPTED 참여자 전체에게 브로드캐스트합니다.
     *
     * <p>쓰로틀: Redis SETNX를 사용해 30초 이내 중복 계산을 원자적으로 차단합니다.</p>
     * <p>stale 방지: putIfNewer로 오래된 결과가 최신 ETA를 덮어쓰지 않도록 합니다.</p>
     *
     * @param travelMode 참여자가 선택한 이동수단 (null이면 TAXI 기본 사용)
     */
    public void calculateAndBroadcast(Long memberId, Long appointmentId,
                                       double latitude, double longitude,
                                       TravelMode travelMode, Appointment appointment,
                                       LocalDateTime now) {
        try {
            // 1. 쓰로틀: 30초 이내 중복 계산 방지 (원자적 SETNX)
            if (!etaCache.tryAcquireCalculation(appointmentId, memberId)) {
                return;
            }

            // 2. ETA 계산: 이동수단 기반 경로 API 호출
            OptionalInt durationMinutes = calculateDurationMinutes(latitude, longitude, travelMode, appointment);
            if (durationMinutes.isEmpty()) {
                log.debug("ETA 계산 불가 — 경로 조회 실패. memberId={}, appointmentId={}", memberId, appointmentId);
                return;
            }

            int minutes = durationMinutes.getAsInt();
            LocalDateTime eta = now.plusMinutes(minutes);

            // 3. 최신 결과만 저장 (오래된 결과가 더 최신 ETA를 덮어쓰지 않도록)
            if (!etaCache.putIfNewer(appointmentId, memberId, eta, minutes, now)) {
                return;
            }

            // 4. ACCEPTED 참여자 전체에게 브로드캐스트
            messagingTemplate.convertAndSend(
                    ETA_TOPIC_PREFIX + appointmentId + ETA_TOPIC_SUFFIX,
                    new EtaBroadcastResponse(memberId, eta, minutes, now));
        } catch (Exception e) {
            log.warn("ETA 계산 중 예기치 않은 오류 발생 memberId={}, appointmentId={}: {}",
                    memberId, appointmentId, e.getMessage());
        }
    }

    private OptionalInt calculateDurationMinutes(double latitude, double longitude,
                                                  TravelMode travelMode, Appointment appointment) {
        // null이면 TAXI 기본 사용 (가장 안정적으로 경로 제공)
        TravelMode mode = travelMode != null ? travelMode : TravelMode.TAXI;
        return switch (mode) {
            case BUS, SUBWAY -> {
                // 대중교통 조회 실패 시 자동차 경로로 폴백
                OptionalInt transit = calculateTransitDuration(latitude, longitude, mode, appointment);
                yield transit.isPresent() ? transit : calculateCarDuration(latitude, longitude, appointment);
            }
            case TAXI -> calculateCarDuration(latitude, longitude, appointment);
        };
    }

    private OptionalInt calculateTransitDuration(double latitude, double longitude,
                                                  TravelMode mode, Appointment appointment) {
        try {
            List<TransitRouteResponse.RouteResponse> routes = transitRouteGetUseCase.getRoutes(
                    new TransitRouteRequest(longitude, latitude,
                            appointment.getDestinationLongitude(), appointment.getDestinationLatitude())
            ).routes();
            Optional<TransitRouteResponse.RouteResponse> fastest = switch (mode) {
                case BUS -> fastestRoute(routes, BUS_ROUTE_TYPE);
                case SUBWAY -> fastestRoute(routes, SUBWAY_ROUTE_TYPE)
                        .or(() -> fastestRoute(routes, BUS_AND_SUBWAY_ROUTE_TYPE));
                default -> Optional.empty();
            };
            return fastest.map(r -> OptionalInt.of(toMinutes(r.totalTime())))
                    .orElse(OptionalInt.empty());
        } catch (BusinessException e) {
            log.warn("대중교통 ETA 계산 실패: {}", e.getErrorCode());
            return OptionalInt.empty();
        }
    }

    private OptionalInt calculateCarDuration(double latitude, double longitude, Appointment appointment) {
        return kakaoMobilityApiClient.getCarRoute(
                longitude, latitude,
                appointment.getDestinationLongitude(), appointment.getDestinationLatitude()
        ).map(route -> OptionalInt.of(toMinutes(route.summary().duration())))
                .orElse(OptionalInt.empty());
    }

    private Optional<TransitRouteResponse.RouteResponse> fastestRoute(
            List<TransitRouteResponse.RouteResponse> routes, String type) {
        return routes.stream()
                .filter(r -> type.equals(r.type()))
                .min(Comparator.comparingInt(TransitRouteResponse.RouteResponse::totalTime));
    }

    private static int toMinutes(int seconds) {
        return (seconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE;
    }
}
