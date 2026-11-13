package com.jigmnnaga.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jigmnnaga.application.dto.response.kakao.KakaoCarRouteResponse;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.DepartureStatus;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.repository.AppointmentParticipantRepository;
import com.jigmnnaga.domain.repository.AppointmentRepository;
import com.jigmnnaga.global.cache.ArrivalProximityCache;
import com.jigmnnaga.global.cache.EtaCache;
import com.jigmnnaga.global.cache.LatenessCache;
import com.jigmnnaga.global.client.kakao.KakaoMobilityApiClient;
import com.jigmnnaga.global.client.kakao.KakaoTransitApiClient;
import com.jigmnnaga.global.util.TimeZones;
import com.jigmnnaga.support.IntegrationTestSupport;
import org.mockito.Mockito;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LocationIntegrationTest extends IntegrationTestSupport {

    @LocalServerPort
    private int port;

    @MockitoBean
    private KakaoMobilityApiClient kakaoMobilityApiClient;

    @MockitoBean
    private KakaoTransitApiClient kakaoTransitApiClient;

    @Autowired
    private EtaCache etaCache;

    @Autowired
    private LatenessCache latenessCache;

    @Autowired
    private ArrivalProximityCache arrivalProximityCache;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentParticipantRepository appointmentParticipantRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void clearRedisState() throws InterruptedException {
        // 이전 테스트의 비동기 WebSocket/ETA 처리가 완료될 때까지 대기합니다.
        // 이전 테스트에서 발송된 메시지 처리가 다음 테스트의 mock에 간섭하는 것을 방지합니다.
        Thread.sleep(300);
        // 이전 테스트 실행에서 남은 Redis 키(ETA 쓰로틀, ETA 결과, 지각, 위치, 도착)를 정리합니다.
        // ETA 쓰로틀 TTL(30초) 이내 재실행 시 키 충돌을 방지합니다.
        deleteKeysByPattern("eta:throttle:*");
        deleteKeysByPattern("eta:result:*");
        deleteKeysByPattern("lateness:*");
        deleteKeysByPattern("location:appointment:*");
        deleteKeysByPattern("arrival:proximity:*");
        deleteKeysByPattern("arrival:claimed:*");
        // 이전 테스트의 지연된 비동기 호출이 @BeforeEach 중 실행되었을 수 있으므로
        // mock 호출 기록을 명시적으로 초기화합니다.
        Mockito.clearInvocations(kakaoMobilityApiClient);
    }

    private void deleteKeysByPattern(String pattern) {
        java.util.Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ── STOMP 헬퍼 ───────────────────────────────────────────────────────────

    private StompSession connect(Member member, BlockingQueue<String> stompErrors) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        StompHeaders headers = new StompHeaders();
        if (member != null) {
            headers.add("Authorization", bearer(member));
        }
        return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), headers,
                new StompSessionHandlerAdapter() {
                    @Override
                    public Type getPayloadType(StompHeaders frameHeaders) {
                        return Map.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders frameHeaders, Object payload) {
                        // STOMP ERROR 프레임 (구독·연결 오류)
                        stompErrors.add(frameHeaders.getFirst("message"));
                    }
                }).get(5, TimeUnit.SECONDS);
    }

    private BlockingQueue<Map<?, ?>> subscribe(StompSession session, String destination) throws InterruptedException {
        BlockingQueue<Map<?, ?>> received = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((Map<?, ?>) payload);
            }
        });
        Thread.sleep(300);
        return received;
    }

    // ── 테스트 셋업 헬퍼 ─────────────────────────────────────────────────────

    private CreatedAppointment confirmedAppointment(Member host) throws Exception {
        CreatedAppointment appointment = createAppointment(host);
        perform(put(appointmentPath(appointment) + "/confirmation/date-time"), host,
                Map.of("date", daysLater(10).toString(), "time", "15:00"));
        perform(put(appointmentPath(appointment) + "/confirmation/location"), host, Map.of(
                "locationId", appointment.locationIds().getFirst(),
                "detailPlace", Map.of("placeName", "강남역 카페거리", "latitude", 37.4979, "longitude", 127.0276)));
        return appointment;
    }

    private void depart(CreatedAppointment appointment, Member member) throws Exception {
        perform(post(appointmentPath(appointment) + "/travel/departure"), member)
                .andExpect(status().isOk());
    }

    private KakaoCarRouteResponse.Route taxiRoute(int durationSeconds) {
        return new KakaoCarRouteResponse.Route(0,
                new KakaoCarRouteResponse.Summary(6710, durationSeconds),
                List.of(new KakaoCarRouteResponse.Section(List.of(
                        new KakaoCarRouteResponse.Road(List.of(127.0, 37.5, 127.05, 37.51))))));
    }

    // ── 위치 공유 테스트 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("DEPARTED 상태인 참여자가 위치를 보내면 구독 중인 모든 참여자에게 전달된다")
    void departedParticipantCanShareLocation() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        CreatedAppointment appointment = confirmedAppointment(host);
        accept(appointment, member);
        depart(appointment, host);

        StompSession hostSession = connect(host, new LinkedBlockingQueue<>());
        StompSession memberSession = connect(member, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> memberLocationQueue = subscribe(memberSession,
                "/sub/appointments/" + appointment.id() + "/location");

        hostSession.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));

        Map<?, ?> received = memberLocationQueue.poll(3, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(((Number) received.get("memberId")).longValue()).isEqualTo(host.getId());
        assertThat(received.get("departureStatus")).isEqualTo("DEPARTED");
        assertThat(received.get("latitude")).isNotNull();
        assertThat(received.get("longitude")).isNotNull();
        assertThat(received.get("updatedAt")).isNotNull();
    }

    @Test
    @DisplayName("위치 송신자도 자신의 채널을 구독하면 자신의 위치 메시지를 받는다")
    void senderAlsoReceivesOwnLocation() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        depart(appointment, host);

        StompSession hostSession = connect(host, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> hostLocationQueue = subscribe(hostSession,
                "/sub/appointments/" + appointment.id() + "/location");

        hostSession.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));

        Map<?, ?> received = hostLocationQueue.poll(3, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(((Number) received.get("memberId")).longValue()).isEqualTo(host.getId());
    }

    @Test
    @DisplayName("출발하지 않은 참여자가 위치를 보내면 /user/queue/errors로 에러를 받는다")
    void nonDepartedParticipantCannotShareLocation() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);

        StompSession hostSession = connect(host, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> errorQueue = subscribe(hostSession, "/user/queue/errors");

        hostSession.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));

        Map<?, ?> error = errorQueue.poll(3, TimeUnit.SECONDS);
        assertThat(error).isNotNull();
        assertThat(error.get("message")).isEqualTo("출발 후에만 위치를 공유할 수 있습니다.");
    }

    @Test
    @DisplayName("약속 참여자가 아닌 사용자는 위치 채널을 구독할 수 없다")
    void nonParticipantCannotSubscribeToLocationChannel() throws Exception {
        Member host = createMember("태양");
        Member outsider = createMember("외부인");
        CreatedAppointment appointment = confirmedAppointment(host);

        BlockingQueue<String> stompErrors = new LinkedBlockingQueue<>();
        StompSession outsiderSession = connect(outsider, stompErrors);
        subscribe(outsiderSession, "/sub/appointments/" + appointment.id() + "/location");

        assertThat(stompErrors.poll(3, TimeUnit.SECONDS)).isEqualTo("약속에 참여한 멤버만 이용할 수 있습니다.");
    }

    @Test
    @DisplayName("1초 이내에 위치를 두 번 보내면 두 번째는 /user/queue/errors로 에러를 받는다")
    void throttlingRejectsTooFrequentUpdates() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        depart(appointment, host);

        StompSession hostSession = connect(host, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> errorQueue = subscribe(hostSession, "/user/queue/errors");

        hostSession.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));
        Thread.sleep(200); // 첫 번째 메시지가 Redis에 저장될 때까지 대기
        hostSession.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4980, "longitude", 127.0277));

        Map<?, ?> error = errorQueue.poll(3, TimeUnit.SECONDS);
        assertThat(error).isNotNull();
        assertThat(error.get("message")).isEqualTo("위치를 너무 자주 업데이트할 수 없습니다.");
    }

    @Test
    @DisplayName("약속이 확정되지 않은 경우 위치 공유를 보내면 /user/queue/errors로 에러를 받는다")
    void locationUpdateFailsWhenAppointmentNotConfirmed() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = createAppointment(host);

        StompSession hostSession = connect(host, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> errorQueue = subscribe(hostSession, "/user/queue/errors");

        hostSession.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));

        Map<?, ?> error = errorQueue.poll(3, TimeUnit.SECONDS);
        assertThat(error).isNotNull();
        assertThat(error.get("success")).isEqualTo(false);
    }

    @Test
    @DisplayName("잘못된 좌표값(위도 범위 초과)으로 위치를 보내면 /user/queue/errors로 에러를 받는다")
    void invalidCoordinatesReturnError() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        depart(appointment, host);

        StompSession hostSession = connect(host, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> errorQueue = subscribe(hostSession, "/user/queue/errors");

        hostSession.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 999.0, "longitude", 127.0276));

        Map<?, ?> error = errorQueue.poll(3, TimeUnit.SECONDS);
        assertThat(error).isNotNull();
        assertThat(error.get("success")).isEqualTo(false);
    }

    // ── ETA 테스트 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("위치 갱신 시 ETA가 계산되어 /eta 채널 구독자에게 브로드캐스트된다")
    void etaBroadcastReceivedBySubscribers() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        CreatedAppointment appointment = confirmedAppointment(host);
        accept(appointment, member);
        depart(appointment, host);

        when(kakaoMobilityApiClient.getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(taxiRoute(900)));

        StompSession hostSession = connect(host, new LinkedBlockingQueue<>());
        StompSession memberSession = connect(member, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> etaQueue = subscribe(memberSession,
                "/sub/appointments/" + appointment.id() + "/eta");

        hostSession.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));

        Map<?, ?> received = etaQueue.poll(3, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(((Number) received.get("memberId")).longValue()).isEqualTo(host.getId());
        assertThat(received.get("remainingMinutes")).isEqualTo(15); // 900초 = 15분
        assertThat(received.get("estimatedArrivalTime")).isNotNull();
        assertThat(received.get("calculatedAt")).isNotNull();
    }

    @Test
    @DisplayName("ETA 결과가 Redis에 저장된다")
    void etaIsSavedInRedis() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        depart(appointment, host);

        when(kakaoMobilityApiClient.getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(taxiRoute(600)));

        StompSession session = connect(host, new LinkedBlockingQueue<>());
        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));
        Thread.sleep(500);

        Optional<LocalDateTime> lastCalc = etaCache.getLastCalculatedAt(appointment.id(), host.getId());
        assertThat(lastCalc).isPresent();
    }

    @Test
    @DisplayName("30초 이내 위치 재갱신 시 외부 ETA API를 중복 호출하지 않는다")
    void etaThrottlePreventsExcessiveApiCalls() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        depart(appointment, host);

        when(kakaoMobilityApiClient.getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(taxiRoute(900)));

        StompSession session = connect(host, new LinkedBlockingQueue<>());

        // 첫 번째 위치 갱신 → ETA 계산
        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));
        Thread.sleep(1500); // 위치 쓰로틀(1s) 해제 대기

        // 두 번째 위치 갱신 → ETA 쓰로틀(30s) 여전히 활성 → API 미호출
        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4980, "longitude", 127.0277));
        Thread.sleep(500);

        verify(kakaoMobilityApiClient, times(1))
                .getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("ETA 쓰로틀 만료 후 위치 갱신 시 ETA가 재계산된다")
    void etaIsRecalculatedAfterThrottleExpires() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        depart(appointment, host);

        when(kakaoMobilityApiClient.getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(taxiRoute(900)));

        StompSession session = connect(host, new LinkedBlockingQueue<>());

        // 첫 번째 위치 갱신 → ETA 계산 (목적지에서 ~1.5km 떨어진 반경 외 위치)
        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.5100, "longitude", 127.0400));
        Thread.sleep(1000); // ETA 계산이 완료될 때까지 충분히 대기

        // Redis에서 쓰로틀 키를 직접 삭제해 30초 경과를 시뮬레이션
        redisTemplate.delete(etaCache.throttleKey(appointment.id(), host.getId()));

        Thread.sleep(1500); // 위치 쓰로틀(1s) 해제 대기

        // 두 번째 위치 갱신 → ETA 재계산 (여전히 반경 외)
        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.5099, "longitude", 127.0399));
        Thread.sleep(1000); // ETA 계산이 완료될 때까지 충분히 대기

        verify(kakaoMobilityApiClient, times(2))
                .getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("경로 API가 실패하면 ETA를 브로드캐스트하지 않는다")
    void etaIsNotBroadcastWhenApiUnavailable() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        depart(appointment, host);

        when(kakaoMobilityApiClient.getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.empty()); // API 실패

        StompSession session = connect(host, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> etaQueue = subscribe(session,
                "/sub/appointments/" + appointment.id() + "/eta");

        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));

        // ETA 브로드캐스트 없음을 확인
        Map<?, ?> received = etaQueue.poll(2, TimeUnit.SECONDS);
        assertThat(received).isNull();
    }

    @Test
    @DisplayName("오래된 ETA 계산 결과는 최신 ETA를 덮어쓰지 않는다")
    void olderEtaDoesNotOverwriteNewerEta() {
        long appointmentId = 99900L + System.currentTimeMillis() % 1000;
        long memberId = 99901L;

        LocalDateTime newerTime = LocalDateTime.now();
        LocalDateTime olderTime = newerTime.minusSeconds(10);

        // 더 최신 결과를 먼저 저장
        boolean storedNewer = etaCache.putIfNewer(appointmentId, memberId,
                newerTime.plusMinutes(20), 20, newerTime);
        assertThat(storedNewer).isTrue();

        // 더 오래된 결과로 덮어쓰기 시도
        boolean storedOlder = etaCache.putIfNewer(appointmentId, memberId,
                olderTime.plusMinutes(25), 25, olderTime);
        assertThat(storedOlder).isFalse();

        // Redis에는 최신 결과가 유지되어야 한다
        Optional<LocalDateTime> lastCalc = etaCache.getLastCalculatedAt(appointmentId, memberId);
        assertThat(lastCalc).contains(newerTime);
    }

    @Test
    @DisplayName("약속 참여자가 아닌 사용자는 ETA 채널을 구독할 수 없다")
    void nonParticipantCannotSubscribeToEtaChannel() throws Exception {
        Member host = createMember("태양");
        Member outsider = createMember("외부인");
        CreatedAppointment appointment = confirmedAppointment(host);

        BlockingQueue<String> stompErrors = new LinkedBlockingQueue<>();
        StompSession outsiderSession = connect(outsider, stompErrors);
        subscribe(outsiderSession, "/sub/appointments/" + appointment.id() + "/eta");

        assertThat(stompErrors.poll(3, TimeUnit.SECONDS)).isEqualTo("약속에 참여한 멤버만 이용할 수 있습니다.");
    }

    @Test
    @DisplayName("ETA 브로드캐스트는 위치 메시지와 채널이 분리되어 있다 (회귀)")
    void etaAndLocationUseSeparateChannels() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        depart(appointment, host);

        when(kakaoMobilityApiClient.getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(taxiRoute(900)));

        StompSession session = connect(host, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> locationQueue = subscribe(session,
                "/sub/appointments/" + appointment.id() + "/location");
        BlockingQueue<Map<?, ?>> etaQueue = subscribe(session,
                "/sub/appointments/" + appointment.id() + "/eta");

        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));

        // 위치 메시지에는 ETA 없음
        Map<?, ?> locMsg = locationQueue.poll(3, TimeUnit.SECONDS);
        assertThat(locMsg).isNotNull();
        assertThat(locMsg.containsKey("estimatedArrivalTime")).isFalse();

        // ETA 메시지에는 위치 없음
        Map<?, ?> etaMsg = etaQueue.poll(3, TimeUnit.SECONDS);
        assertThat(etaMsg).isNotNull();
        assertThat(etaMsg.containsKey("latitude")).isFalse();
    }

    // ── 지각 감지 테스트 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("약속 10일 후 ETA 계산 시 지각 없음(0, false)으로 브로드캐스트된다")
    void etaBroadcastIncludesLatenessFieldsWhenOnTime() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host); // 약속 10일 후
        depart(appointment, host);

        // ETA 15분 → 약속시간(10일 후)보다 훨씬 일찍 도착
        when(kakaoMobilityApiClient.getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(taxiRoute(900)));

        StompSession session = connect(host, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> etaQueue = subscribe(session,
                "/sub/appointments/" + appointment.id() + "/eta");

        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));

        Map<?, ?> received = etaQueue.poll(3, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(((Number) received.get("expectedLatenessMinutes")).intValue()).isEqualTo(0);
        assertThat(received.get("isExpectedLate")).isEqualTo(false);
    }

    @Test
    @DisplayName("약속 시간을 과거로 설정 시 ETA가 약속 시간 초과하여 지각으로 감지된다")
    void etaBroadcastIndicatesLatenessWhenEtaExceedsAppointmentTime() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        depart(appointment, host);

        // 약속 시간을 과거 10분 전으로 변경 (지각 상황 시뮬레이션)
        LocalDateTime pastAppointment = LocalDateTime.now(TimeZones.SEOUL).minusMinutes(10);
        transactionTemplate.execute(status -> {
            Appointment appt = appointmentRepository.findById(appointment.id()).orElseThrow();
            appt.confirmDateTime(pastAppointment.toLocalDate(), pastAppointment.toLocalTime().withSecond(0).withNano(0));
            return null;
        });

        // ETA 1800초(30분) → 약속시간(과거 10분 전) 기준으로 ~40분 지각
        when(kakaoMobilityApiClient.getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(taxiRoute(1800)));

        StompSession session = connect(host, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> etaQueue = subscribe(session,
                "/sub/appointments/" + appointment.id() + "/eta");

        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));

        Map<?, ?> received = etaQueue.poll(3, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.get("isExpectedLate")).isEqualTo(true);
        assertThat(((Number) received.get("expectedLatenessMinutes")).intValue()).isGreaterThan(0);
    }

    @Test
    @DisplayName("ETA 계산 후 LatenessCache에 지각 정보가 저장된다")
    void latenessInfoIsSavedInRedisAfterEtaCalculation() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host); // 약속 10일 후
        depart(appointment, host);

        when(kakaoMobilityApiClient.getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(taxiRoute(900)));

        StompSession session = connect(host, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> etaQueue = subscribe(session,
                "/sub/appointments/" + appointment.id() + "/eta");

        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));

        // ETA 브로드캐스트를 기다려 ETA 계산 완료를 확인
        Map<?, ?> received = etaQueue.poll(3, TimeUnit.SECONDS);
        assertThat(received).isNotNull();

        java.util.Optional<LatenessCache.LatenessInfo> info = latenessCache.get(appointment.id(), host.getId());
        assertThat(info).isPresent();
        assertThat(info.get().isExpectedLate()).isFalse(); // 약속 10일 후이므로 지각 아님
    }

    @Test
    @DisplayName("지각 감지 추가 후 기존 ETA 브로드캐스트 필드들이 여전히 올바르다 (회귀)")
    void existingEtaFeaturesAreUnaffectedByLatenessDetection() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        CreatedAppointment appointment = confirmedAppointment(host);
        accept(appointment, member);
        depart(appointment, host);

        when(kakaoMobilityApiClient.getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(taxiRoute(900)));

        StompSession hostSession = connect(host, new LinkedBlockingQueue<>());
        StompSession memberSession = connect(member, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> etaQueue = subscribe(memberSession,
                "/sub/appointments/" + appointment.id() + "/eta");

        hostSession.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));

        Map<?, ?> received = etaQueue.poll(3, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.get("remainingMinutes")).isEqualTo(15); // 900초 = 15분
        assertThat(received.get("estimatedArrivalTime")).isNotNull();
        assertThat(received.get("calculatedAt")).isNotNull();
        assertThat(((Number) received.get("memberId")).longValue()).isEqualTo(host.getId());
    }

    // ── 도착 감지 테스트 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("목적지_반경_외_위치_전송_시_도착_감지되지_않는다")
    void 목적지_반경_외_위치_전송_시_도착_감지되지_않는다() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        depart(appointment, host);

        StompSession session = connect(host, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> arrivalQueue = subscribe(session,
                "/sub/appointments/" + appointment.id() + "/arrival");

        // 목적지(37.4979, 127.0276)에서 약 1.5km 떨어진 위치
        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.5100, "longitude", 127.0400));
        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.5100, "longitude", 127.0400));

        Map<?, ?> arrivalMsg = arrivalQueue.poll(2, TimeUnit.SECONDS);
        assertThat(arrivalMsg).isNull();

        // DB 상태도 DEPARTED 유지
        Thread.sleep(300);
        AppointmentParticipant participant = appointmentParticipantRepository
                .findByAppointmentIdAndMemberId(appointment.id(), host.getId()).orElseThrow();
        assertThat(participant.getDepartureStatus()).isEqualTo(DepartureStatus.DEPARTED);
    }

    @Test
    @DisplayName("목적지_반경_내_1회만_전송_시_도착_감지되지_않는다")
    void 목적지_반경_내_1회만_전송_시_도착_감지되지_않는다() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        depart(appointment, host);

        StompSession session = connect(host, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> arrivalQueue = subscribe(session,
                "/sub/appointments/" + appointment.id() + "/arrival");

        // 목적지와 동일한 좌표 → 반경 내 (1회)
        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));

        Map<?, ?> arrivalMsg = arrivalQueue.poll(2, TimeUnit.SECONDS);
        assertThat(arrivalMsg).isNull();

        Thread.sleep(300);
        AppointmentParticipant participant = appointmentParticipantRepository
                .findByAppointmentIdAndMemberId(appointment.id(), host.getId()).orElseThrow();
        assertThat(participant.getDepartureStatus()).isEqualTo(DepartureStatus.DEPARTED);
    }

    @Test
    @DisplayName("목적지_반경_내_2회_연속_전송_시_ARRIVED로_변경되고_arrival_채널에_브로드캐스트된다")
    void 목적지_반경_내_2회_연속_전송_시_ARRIVED로_변경되고_arrival_채널에_브로드캐스트된다() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        CreatedAppointment appointment = confirmedAppointment(host);
        accept(appointment, member);
        depart(appointment, host);

        StompSession hostSession = connect(host, new LinkedBlockingQueue<>());
        StompSession memberSession = connect(member, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> arrivalQueue = subscribe(memberSession,
                "/sub/appointments/" + appointment.id() + "/arrival");

        // 첫 번째 위치 (반경 내)
        hostSession.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));
        Thread.sleep(1200); // 위치 쓰로틀(1s) 해제 대기

        // 두 번째 위치 (반경 내) → 도착 감지
        hostSession.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));

        Map<?, ?> arrivalMsg = arrivalQueue.poll(3, TimeUnit.SECONDS);
        assertThat(arrivalMsg).isNotNull();
        assertThat(((Number) arrivalMsg.get("memberId")).longValue()).isEqualTo(host.getId());
        assertThat(arrivalMsg.get("arrivedAt")).isNotNull();

        // DB 상태 확인
        Thread.sleep(300);
        AppointmentParticipant participant = transactionTemplate.execute(status ->
                appointmentParticipantRepository
                        .findByAppointmentIdAndMemberId(appointment.id(), host.getId()).orElseThrow());
        assertThat(participant.getDepartureStatus()).isEqualTo(DepartureStatus.ARRIVED);
    }

    @Test
    @DisplayName("반경_외_진입_후_다시_반경_내_2회_전송_시_도착_감지된다 (카운트_리셋_후_재누적)")
    void 반경_외_진입_후_다시_반경_내_2회_전송_시_도착_감지된다() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        depart(appointment, host);

        StompSession session = connect(host, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> arrivalQueue = subscribe(session,
                "/sub/appointments/" + appointment.id() + "/arrival");

        // 1. 반경 내 1회
        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));
        Thread.sleep(1200);

        // 2. 반경 외 → 카운트 초기화
        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.5100, "longitude", 127.0400));
        Thread.sleep(1200);

        // 3. 반경 내 1회 (카운트 1)
        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));
        Thread.sleep(1200);

        // 중간에 arrival 없음 확인
        assertThat(arrivalQueue.poll(100, TimeUnit.MILLISECONDS)).isNull();

        // 4. 반경 내 2회 (카운트 2) → 도착 감지
        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));

        Map<?, ?> arrivalMsg = arrivalQueue.poll(3, TimeUnit.SECONDS);
        assertThat(arrivalMsg).isNotNull();
        assertThat(((Number) arrivalMsg.get("memberId")).longValue()).isEqualTo(host.getId());
    }

    @Test
    @DisplayName("도착_감지_직후_ETA_계산을_하지_않는다")
    void 도착_감지_직후_ETA_계산을_하지_않는다() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = confirmedAppointment(host);
        depart(appointment, host);

        when(kakaoMobilityApiClient.getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(taxiRoute(900)));

        StompSession session = connect(host, new LinkedBlockingQueue<>());

        // 첫 번째 위치 (반경 내 1회) — ETA 호출됨 (카운트=1, 도착 미감지)
        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));
        Thread.sleep(1200);
        verify(kakaoMobilityApiClient, times(1))
                .getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble());

        // 두 번째 위치 (반경 내 2회) — 도착 감지! arrived=true → ETA 미호출
        redisTemplate.delete(etaCache.throttleKey(appointment.id(), host.getId()));
        session.send("/pub/appointments/" + appointment.id() + "/location",
                Map.of("latitude", 37.4979, "longitude", 127.0276));
        Thread.sleep(500);

        // 도착 감지 직후는 ETA를 계산하지 않으므로 여전히 총 1회
        verify(kakaoMobilityApiClient, times(1))
                .getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("도착_채널_구독_가능_회귀_테스트")
    void 도착_채널_구독_가능_회귀_테스트() throws Exception {
        Member host = createMember("태양");
        Member outsider = createMember("외부인");
        CreatedAppointment appointment = confirmedAppointment(host);

        // 참여자는 구독 가능
        BlockingQueue<String> hostErrors = new LinkedBlockingQueue<>();
        StompSession hostSession = connect(host, hostErrors);
        subscribe(hostSession, "/sub/appointments/" + appointment.id() + "/arrival");
        assertThat(hostErrors.poll(1, TimeUnit.SECONDS)).isNull();

        // 비참여자는 구독 불가
        BlockingQueue<String> outsiderErrors = new LinkedBlockingQueue<>();
        StompSession outsiderSession = connect(outsider, outsiderErrors);
        subscribe(outsiderSession, "/sub/appointments/" + appointment.id() + "/arrival");
        assertThat(outsiderErrors.poll(3, TimeUnit.SECONDS)).isEqualTo("약속에 참여한 멤버만 이용할 수 있습니다.");
    }
}
