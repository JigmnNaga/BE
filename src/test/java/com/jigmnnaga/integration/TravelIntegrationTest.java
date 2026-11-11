package com.jigmnnaga.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jigmnnaga.application.dto.response.kakao.KakaoCarRouteResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoTransitRouteResponse;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.global.client.kakao.KakaoMobilityApiClient;
import com.jigmnnaga.global.client.kakao.KakaoTransitApiClient;
import com.jigmnnaga.support.IntegrationTestSupport;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

class TravelIntegrationTest extends IntegrationTestSupport {

    @MockitoBean
    private KakaoTransitApiClient kakaoTransitApiClient;

    @MockitoBean
    private KakaoMobilityApiClient kakaoMobilityApiClient;

    private final double latitude = 37.4 + ThreadLocalRandom.current().nextDouble(0.1);
    private final double longitude = 127.0 + ThreadLocalRandom.current().nextDouble(0.1);

    private KakaoTransitRouteResponse.Route transitRoute(String type, int totalSeconds, List<String[]> steps) {
        return new KakaoTransitRouteResponse.Route(
                new KakaoTransitRouteResponse.RouteProperties(type, 5000, totalSeconds, 0),
                steps.stream().map(step -> new KakaoTransitRouteResponse.Step(
                        new KakaoTransitRouteResponse.StepProperties("안내", step[0], 100, Integer.parseInt(step[1]),
                                List.of(), step.length > 2 ? List.of(new KakaoTransitRouteResponse.Vehicle(step[2], step[0])) : List.of()),
                        new KakaoTransitRouteResponse.Path(List.of(List.of(127.0, 37.5), List.of(127.01, 37.51)))))
                        .toList());
    }

    private void givenTransitRoutes(KakaoTransitRouteResponse.Route... routes) {
        when(kakaoTransitApiClient.getRoutes(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new KakaoTransitRouteResponse("OK", null, List.of(routes)));
    }

    private void givenTaxiRoute(int durationSeconds) {
        when(kakaoMobilityApiClient.getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(new KakaoCarRouteResponse.Route(0,
                        new KakaoCarRouteResponse.Summary(6710, durationSeconds),
                        List.of(new KakaoCarRouteResponse.Section(List.of(
                                new KakaoCarRouteResponse.Road(List.of(127.0, 37.5, 127.05, 37.51))))))));
    }

    private CreatedAppointment confirmedAppointment(Member host) throws Exception {
        CreatedAppointment appointment = createAppointment(host);
        perform(put(appointmentPath(appointment) + "/confirmation/date-time"), host,
                Map.of("date", daysLater(10).toString(), "time", "15:00"));
        perform(put(appointmentPath(appointment) + "/confirmation/location"), host, Map.of(
                "locationId", appointment.locationIds().getFirst(),
                "detailPlace", Map.of("placeName", "강남역 카페거리", "latitude", 37.4979, "longitude", 127.0276)));
        return appointment;
    }

    private ResultActions getOptions(CreatedAppointment appointment, Member member) throws Exception {
        return perform(get(appointmentPath(appointment) + "/travel/options")
                .param("latitude", String.valueOf(latitude))
                .param("longitude", String.valueOf(longitude)), member);
    }

    @Test
    @DisplayName("세부 장소가 확정되기 전에는 이동수단 후보를 조회할 수 없다")
    void requiresConfirmedAppointment() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = createAppointment(host);

        getOptions(appointment, host).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("버스·지하철은 가장 빠른 경로를, 지하철 전용 경로가 없으면 버스+지하철 경로를, 택시는 자동차 경로를 반환한다")
    void travelOptions() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        givenTransitRoutes(
                transitRoute("BUS", 1500, List.of(new String[]{"WALKING", "300"}, new String[]{"BUS", "900", "360"})),
                transitRoute("BUS", 1190, List.of(new String[]{"WALKING", "290"}, new String[]{"BUS", "480", "341"},
                        new String[]{"WALKING", "420"})),
                transitRoute("BUS_AND_SUBWAY", 1450, List.of(new String[]{"SUBWAY", "720", "2호선"},
                        new String[]{"BUS", "600", "3314"})));
        givenTaxiRoute(900);
        LocalDateTime before = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        JsonNode data = data(getOptions(appointment, host).andExpect(status().isOk()));

        assertThat(data.get("destinationName").asString()).isEqualTo("강남역 카페거리");
        JsonNode options = data.get("options");
        assertThat(options).hasSize(3);
        assertThat(options.get(0).get("mode").asString()).isEqualTo("BUS");
        assertThat(options.get(0).get("durationMinutes").asInt()).isEqualTo(20);
        assertThat(options.get(0).get("steps").get(1).get("name").asString()).isEqualTo("341");
        assertThat(options.get(1).get("mode").asString()).isEqualTo("SUBWAY");
        assertThat(options.get(1).get("durationMinutes").asInt()).isEqualTo(25);
        assertThat(options.get(2).get("mode").asString()).isEqualTo("TAXI");
        assertThat(options.get(2).get("durationMinutes").asInt()).isEqualTo(15);
        assertThat(options.get(2).get("path")).hasSize(2);
        assertThat(LocalDateTime.parse(options.get(2).get("estimatedArrivalTime").asString()))
                .isAfterOrEqualTo(before.plusMinutes(15).withNano(0));
    }

    @Test
    @DisplayName("대중교통 경로 조회가 실패해도 택시 후보는 반환한다")
    void partialOptionsWhenTransitFails() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        when(kakaoTransitApiClient.getRoutes(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenThrow(new com.jigmnnaga.application.exception.BusinessException(
                        com.jigmnnaga.application.exception.ErrorCode.TRANSIT_ROUTE_FAILED));
        givenTaxiRoute(600);

        getOptions(appointment, host)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.options.length()").value(1))
                .andExpect(jsonPath("$.data.options[0].mode").value("TAXI"));
    }

    @Test
    @DisplayName("등록된 출발 위치 기준으로 예상 이동시간을 조회한다")
    void estimatedTravelTime() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        perform(put(appointmentPath(appointment) + "/travel/departure-location"), host,
                Map.of("name", "우리집", "latitude", latitude, "longitude", longitude));
        givenTransitRoutes(
                transitRoute("BUS", 1190, List.of(new String[]{"WALKING", "290"}, new String[]{"BUS", "480", "341"})));
        givenTaxiRoute(900);

        JsonNode data = data(perform(
                get(appointmentPath(appointment) + "/travel/estimated-time"), host)
                .andExpect(status().isOk()));

        assertThat(data.get("destinationName").asString()).isEqualTo("강남역 카페거리");
        JsonNode options = data.get("options");
        assertThat(options).hasSize(2);
        assertThat(options.get(0).get("mode").asString()).isEqualTo("BUS");
        assertThat(options.get(0).get("durationMinutes").asInt()).isEqualTo(20);
        assertThat(options.get(1).get("mode").asString()).isEqualTo("TAXI");
    }

    @Test
    @DisplayName("출발 위치를 수정하면 변경된 좌표로 예상 이동시간이 계산된다")
    void estimatedTravelTimeAfterUpdateDepartureLocation() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        perform(put(appointmentPath(appointment) + "/travel/departure-location"), host,
                Map.of("name", "우리집", "latitude", latitude, "longitude", longitude));

        double updatedLatitude = 37.5665;
        double updatedLongitude = 126.9780;
        perform(put(appointmentPath(appointment) + "/travel/departure-location"), host,
                Map.of("name", "서울시청", "latitude", updatedLatitude, "longitude", updatedLongitude));

        givenTransitRoutes(
                transitRoute("SUBWAY", 1800, List.<String[]>of(new String[]{"SUBWAY", "1500", "2호선"})));
        givenTaxiRoute(1200);

        JsonNode data = data(perform(
                get(appointmentPath(appointment) + "/travel/estimated-time"), host)
                .andExpect(status().isOk()));

        assertThat(data.get("options").get(0).get("mode").asString()).isEqualTo("SUBWAY");
        assertThat(data.get("options").get(0).get("durationMinutes").asInt()).isEqualTo(30);
    }

    @Test
    @DisplayName("출발 위치가 미등록 상태에서 예상 이동시간을 조회하면 400을 반환한다")
    void estimatedTravelTimeWithoutDepartureLocation() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);

        perform(get(appointmentPath(appointment) + "/travel/estimated-time"), host)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("등록된 출발 위치 기준 이동시간 조회 시 대중교통이 실패해도 택시는 반환한다")
    void estimatedTravelTimePartialWhenTransitFails() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        perform(put(appointmentPath(appointment) + "/travel/departure-location"), host,
                Map.of("name", "우리집", "latitude", latitude, "longitude", longitude));
        when(kakaoTransitApiClient.getRoutes(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenThrow(new com.jigmnnaga.application.exception.BusinessException(
                        com.jigmnnaga.application.exception.ErrorCode.TRANSIT_ROUTE_FAILED));
        givenTaxiRoute(600);

        perform(get(appointmentPath(appointment) + "/travel/estimated-time"), host)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.options.length()").value(1))
                .andExpect(jsonPath("$.data.options[0].mode").value("TAXI"));
    }

    @Test
    @DisplayName("기존 GET /travel/options는 현재 위치 파라미터 기반으로 동일하게 동작한다 (회귀)")
    void travelOptionsRegression() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        givenTransitRoutes(
                transitRoute("BUS", 1190, List.<String[]>of(new String[]{"BUS", "1190", "341"})));
        givenTaxiRoute(900);

        JsonNode data = data(getOptions(appointment, host).andExpect(status().isOk()));

        assertThat(data.get("destinationName").asString()).isEqualTo("강남역 카페거리");
        assertThat(data.get("options")).hasSize(2);
        assertThat(data.get("options").get(0).get("mode").asString()).isEqualTo("BUS");
    }

    @Test
    @DisplayName("이동수단을 선택하면 내 이동수단과 멤버 이동수단에 반영되고, 참여자가 아니면 선택할 수 없다")
    void selectTravelMode() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        Member outsider = createMember("외부인");
        CreatedAppointment appointment = confirmedAppointment(host);
        accept(appointment, member);
        String path = appointmentPath(appointment) + "/travel/mode";

        perform(put(path), member, Map.of("mode", "BUS", "durationMinutes", 20)).andExpect(status().isOk());

        perform(get(appointmentPath(appointment)), member).andExpect(jsonPath("$.data.myTravelMode").value("BUS"));
        perform(get(appointmentPath(appointment)), host)
                .andExpect(jsonPath("$.data.myTravelMode").isEmpty())
                .andExpect(jsonPath("$.data.members[1].travelMode").value("BUS"));
        perform(put(path), outsider, Map.of("mode", "BUS", "durationMinutes", 20)).andExpect(status().isForbidden());
        perform(put(path), member, Map.of("mode", "BUS", "durationMinutes", 0)).andExpect(status().isBadRequest());
    }
}
