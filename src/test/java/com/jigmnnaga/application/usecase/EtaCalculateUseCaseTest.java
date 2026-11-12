package com.jigmnnaga.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jigmnnaga.application.dto.response.EtaBroadcastResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoCarRouteResponse;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.TravelMode;
import com.jigmnnaga.global.cache.EtaCache;
import com.jigmnnaga.global.cache.LatenessCache;
import com.jigmnnaga.global.client.kakao.KakaoMobilityApiClient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class EtaCalculateUseCaseTest {

    @Mock
    private TransitRouteGetUseCase transitRouteGetUseCase;

    @Mock
    private KakaoMobilityApiClient kakaoMobilityApiClient;

    @Mock
    private EtaCache etaCache;

    @Mock
    private LatenessCache latenessCache;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private EtaCalculateUseCase useCase;

    // ── calculateLatenessMinutes 직접 테스트 ─────────────────────────────────

    @Test
    @DisplayName("예상_도착_시각이_약속_시간보다_이전이면_0분")
    void 예상_도착_시각이_약속_시간보다_이전이면_0분() {
        LocalDateTime eta = LocalDateTime.of(2024, 1, 15, 14, 50, 0);
        LocalDateTime confirmed = LocalDateTime.of(2024, 1, 15, 15, 0, 0);

        int result = EtaCalculateUseCase.calculateLatenessMinutes(eta, confirmed);

        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("예상_도착_시각이_약속_시간과_정확히_일치하면_0분")
    void 예상_도착_시각이_약속_시간과_정확히_일치하면_0분() {
        LocalDateTime eta = LocalDateTime.of(2024, 1, 15, 15, 0, 0);
        LocalDateTime confirmed = LocalDateTime.of(2024, 1, 15, 15, 0, 0);

        int result = EtaCalculateUseCase.calculateLatenessMinutes(eta, confirmed);

        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("예상_도착_시각이_약속_시간보다_늦으면_지각_분_반환")
    void 예상_도착_시각이_약속_시간보다_늦으면_지각_분_반환() {
        LocalDateTime eta = LocalDateTime.of(2024, 1, 15, 15, 10, 0);
        LocalDateTime confirmed = LocalDateTime.of(2024, 1, 15, 15, 0, 0);

        int result = EtaCalculateUseCase.calculateLatenessMinutes(eta, confirmed);

        assertThat(result).isEqualTo(10);
    }

    @Test
    @DisplayName("1분_미만_지각은_올림하여_1분 (30초 → 1분)")
    void 삼십초_지각은_올림하여_1분() {
        LocalDateTime eta = LocalDateTime.of(2024, 1, 15, 15, 0, 30);
        LocalDateTime confirmed = LocalDateTime.of(2024, 1, 15, 15, 0, 0);

        int result = EtaCalculateUseCase.calculateLatenessMinutes(eta, confirmed);

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("1초_지각도_올림하여_1분 (1초 → 1분)")
    void 일초_지각도_올림하여_1분() {
        LocalDateTime eta = LocalDateTime.of(2024, 1, 15, 15, 0, 1);
        LocalDateTime confirmed = LocalDateTime.of(2024, 1, 15, 15, 0, 0);

        int result = EtaCalculateUseCase.calculateLatenessMinutes(eta, confirmed);

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("59초_지각은_1분 (59초 → 1분)")
    void 오십구초_지각은_1분() {
        LocalDateTime eta = LocalDateTime.of(2024, 1, 15, 15, 0, 59);
        LocalDateTime confirmed = LocalDateTime.of(2024, 1, 15, 15, 0, 0);

        int result = EtaCalculateUseCase.calculateLatenessMinutes(eta, confirmed);

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("정확히_60초_지각은_1분")
    void 정확히_60초_지각은_1분() {
        LocalDateTime eta = LocalDateTime.of(2024, 1, 15, 15, 1, 0);
        LocalDateTime confirmed = LocalDateTime.of(2024, 1, 15, 15, 0, 0);

        int result = EtaCalculateUseCase.calculateLatenessMinutes(eta, confirmed);

        assertThat(result).isEqualTo(1);
    }

    // ── Mock 기반 전체 흐름 테스트 ───────────────────────────────────────────

    @Test
    @DisplayName("ETA_계산_불가시_지각_캐시를_갱신하지_않는다")
    void ETA_계산_불가시_지각_캐시를_갱신하지_않는다() {
        Appointment appointment = mockAppointment(127.0, 37.5);
        when(kakaoMobilityApiClient.getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());
        when(etaCache.tryAcquireCalculation(anyLong(), anyLong())).thenReturn(true);

        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 14, 50, 0);
        useCase.calculateAndBroadcast(1L, 100L, 37.4979, 127.0276, TravelMode.TAXI, appointment, now);

        verify(latenessCache, never()).putIfNewer(anyLong(), anyLong(), anyInt(), any(Boolean.class), any());
    }

    @Test
    @DisplayName("약속_시간_미확정시_지각_필드를_null로_브로드캐스트한다")
    void 약속_시간_미확정시_지각_필드를_null로_브로드캐스트한다() {
        Appointment appointment = mockAppointment(127.0, 37.5);
        when(appointment.getConfirmedDateTime()).thenReturn(null);
        when(kakaoMobilityApiClient.getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(taxiRoute(600)));
        when(etaCache.tryAcquireCalculation(anyLong(), anyLong())).thenReturn(true);
        when(etaCache.putIfNewer(anyLong(), anyLong(), any(), anyInt(), any())).thenReturn(true);

        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 14, 50, 0);
        useCase.calculateAndBroadcast(1L, 100L, 37.4979, 127.0276, TravelMode.TAXI, appointment, now);

        verify(latenessCache, never()).putIfNewer(anyLong(), anyLong(), anyInt(), any(Boolean.class), any());

        ArgumentCaptor<EtaBroadcastResponse> captor = ArgumentCaptor.forClass(EtaBroadcastResponse.class);
        verify(messagingTemplate).convertAndSend(any(String.class), captor.capture());
        EtaBroadcastResponse response = captor.getValue();
        assertThat(response.expectedLatenessMinutes()).isNull();
        assertThat(response.isExpectedLate()).isNull();
    }

    @Test
    @DisplayName("최신성_검증_실패시_지각_캐시를_갱신하지_않는다")
    void 최신성_검증_실패시_지각_캐시를_갱신하지_않는다() {
        Appointment appointment = mockAppointment(127.0, 37.5);
        when(kakaoMobilityApiClient.getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(taxiRoute(600)));
        when(etaCache.tryAcquireCalculation(anyLong(), anyLong())).thenReturn(true);
        when(etaCache.putIfNewer(anyLong(), anyLong(), any(), anyInt(), any())).thenReturn(false);

        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 14, 50, 0);
        useCase.calculateAndBroadcast(1L, 100L, 37.4979, 127.0276, TravelMode.TAXI, appointment, now);

        verify(latenessCache, never()).putIfNewer(anyLong(), anyLong(), anyInt(), any(Boolean.class), any());
    }

    @Test
    @DisplayName("예상_지각_상태일때_지각_캐시와_브로드캐스트를_갱신한다")
    void 예상_지각_상태일때_지각_캐시와_브로드캐스트를_갱신한다() {
        // 약속시간: 2024-01-15T15:00:00, now: 2024-01-15T14:50:00
        // ETA duration: 1200초(20분) → eta: 2024-01-15T15:10:00 → 10분 지각
        Long memberId = 1L;
        Long appointmentId = 100L;
        LocalDateTime confirmedDateTime = LocalDateTime.of(2024, 1, 15, 15, 0, 0);
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 14, 50, 0);

        Appointment appointment = mockAppointment(127.0, 37.5);
        when(appointment.getConfirmedDateTime()).thenReturn(confirmedDateTime);
        when(kakaoMobilityApiClient.getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(taxiRoute(1200)));
        when(etaCache.tryAcquireCalculation(anyLong(), anyLong())).thenReturn(true);
        when(etaCache.putIfNewer(anyLong(), anyLong(), any(), anyInt(), any())).thenReturn(true);

        useCase.calculateAndBroadcast(memberId, appointmentId, 37.4979, 127.0276, TravelMode.TAXI, appointment, now);

        verify(latenessCache).putIfNewer(eq(appointmentId), eq(memberId), eq(10), eq(true), eq(now));

        ArgumentCaptor<EtaBroadcastResponse> captor = ArgumentCaptor.forClass(EtaBroadcastResponse.class);
        verify(messagingTemplate).convertAndSend(any(String.class), captor.capture());
        EtaBroadcastResponse response = captor.getValue();
        assertThat(response.expectedLatenessMinutes()).isEqualTo(10);
        assertThat(response.isExpectedLate()).isTrue();
    }

    @Test
    @DisplayName("정시_도착_예상시_지각_상태를_해제한다")
    void 정시_도착_예상시_지각_상태를_해제한다() {
        // 약속시간: 2024-01-15T15:30:00, now: 2024-01-15T15:00:00
        // ETA duration: 1200초(20분) → eta: 2024-01-15T15:20:00 → 약속보다 10분 일찍
        Long memberId = 1L;
        Long appointmentId = 100L;
        LocalDateTime confirmedDateTime = LocalDateTime.of(2024, 1, 15, 15, 30, 0);
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 15, 0, 0);

        Appointment appointment = mockAppointment(127.0, 37.5);
        when(appointment.getConfirmedDateTime()).thenReturn(confirmedDateTime);
        when(kakaoMobilityApiClient.getCarRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(taxiRoute(1200)));
        when(etaCache.tryAcquireCalculation(anyLong(), anyLong())).thenReturn(true);
        when(etaCache.putIfNewer(anyLong(), anyLong(), any(), anyInt(), any())).thenReturn(true);

        useCase.calculateAndBroadcast(memberId, appointmentId, 37.4979, 127.0276, TravelMode.TAXI, appointment, now);

        verify(latenessCache).putIfNewer(eq(appointmentId), eq(memberId), eq(0), eq(false), eq(now));

        ArgumentCaptor<EtaBroadcastResponse> captor = ArgumentCaptor.forClass(EtaBroadcastResponse.class);
        verify(messagingTemplate).convertAndSend(any(String.class), captor.capture());
        EtaBroadcastResponse response = captor.getValue();
        assertThat(response.expectedLatenessMinutes()).isEqualTo(0);
        assertThat(response.isExpectedLate()).isFalse();
    }

    // ── 헬퍼 메서드 ─────────────────────────────────────────────────────────

    private Appointment mockAppointment(double longitude, double latitude) {
        Appointment appointment = org.mockito.Mockito.mock(Appointment.class);
        when(appointment.getDestinationLongitude()).thenReturn(longitude);
        when(appointment.getDestinationLatitude()).thenReturn(latitude);
        return appointment;
    }

    private KakaoCarRouteResponse.Route taxiRoute(int durationSeconds) {
        return new KakaoCarRouteResponse.Route(0,
                new KakaoCarRouteResponse.Summary(6710, durationSeconds),
                List.of(new KakaoCarRouteResponse.Section(List.of(
                        new KakaoCarRouteResponse.Road(List.of(127.0, 37.5, 127.05, 37.51))))));
    }
}
