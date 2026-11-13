package com.jigmnnaga.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jigmnnaga.application.dto.response.ArrivalBroadcastResponse;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.DepartureStatus;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.global.cache.ArrivalProximityCache;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class ArrivalDetectUseCaseTest {

    @Mock
    private ArrivalProximityCache arrivalProximityCache;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ArrivalDetectUseCase useCase;

    private static final Long APPOINTMENT_ID = 100L;
    private static final Long MEMBER_ID = 1L;
    // 목적지 좌표: 강남역 (37.4979, 127.0276)
    private static final double DEST_LAT = 37.4979;
    private static final double DEST_LON = 127.0276;

    @Test
    @DisplayName("이미_ARRIVED_상태이면_감지를_건너뛴다")
    void 이미_ARRIVED_상태이면_감지를_건너뛴다() {
        AppointmentParticipant participant = mockArrivedParticipant();

        boolean result = useCase.detectAndArrive(participant, APPOINTMENT_ID, DEST_LAT, DEST_LON,
                LocalDateTime.now());

        assertThat(result).isTrue();
        verify(arrivalProximityCache, never()).incrementCount(anyLong(), anyLong());
        verify(arrivalProximityCache, never()).resetCount(anyLong(), anyLong());
    }

    @Test
    @DisplayName("반경_외에서는_카운트를_초기화하고_false를_반환한다")
    void 반경_외에서는_카운트를_초기화하고_false를_반환한다() {
        AppointmentParticipant participant = mockParticipant(DepartureStatus.DEPARTED);
        // 목적지에서 약 500m 떨어진 위치
        double farLat = 37.5024;
        double farLon = 127.0276;

        boolean result = useCase.detectAndArrive(participant, APPOINTMENT_ID, farLat, farLon,
                LocalDateTime.now());

        assertThat(result).isFalse();
        verify(arrivalProximityCache).resetCount(APPOINTMENT_ID, MEMBER_ID);
        verify(arrivalProximityCache, never()).incrementCount(anyLong(), anyLong());
    }

    @Test
    @DisplayName("반경_내_1회_관측은_카운트만_증가하고_도착_미처리")
    void 반경_내_1회_관측은_카운트만_증가하고_도착_미처리() {
        AppointmentParticipant participant = mockParticipant(DepartureStatus.DEPARTED);
        when(arrivalProximityCache.incrementCount(APPOINTMENT_ID, MEMBER_ID)).thenReturn(1L);

        boolean result = useCase.detectAndArrive(participant, APPOINTMENT_ID, DEST_LAT, DEST_LON,
                LocalDateTime.now());

        assertThat(result).isFalse();
        verify(arrivalProximityCache, never()).tryClaimArrival(anyLong(), anyLong());
    }

    @Test
    @DisplayName("반경_내_2회_연속_관측_시_클레임을_시도한다")
    void 반경_내_2회_연속_관측_시_클레임을_시도한다() {
        AppointmentParticipant participant = mockParticipant(DepartureStatus.DEPARTED);
        when(arrivalProximityCache.incrementCount(APPOINTMENT_ID, MEMBER_ID)).thenReturn(2L);
        when(arrivalProximityCache.tryClaimArrival(APPOINTMENT_ID, MEMBER_ID)).thenReturn(false);

        useCase.detectAndArrive(participant, APPOINTMENT_ID, DEST_LAT, DEST_LON, LocalDateTime.now());

        verify(arrivalProximityCache).tryClaimArrival(APPOINTMENT_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("클레임_실패_시_false_반환하고_arrive_미호출")
    void 클레임_실패_시_false_반환하고_arrive_미호출() {
        AppointmentParticipant participant = mockParticipant(DepartureStatus.DEPARTED);
        when(arrivalProximityCache.incrementCount(APPOINTMENT_ID, MEMBER_ID)).thenReturn(2L);
        when(arrivalProximityCache.tryClaimArrival(APPOINTMENT_ID, MEMBER_ID)).thenReturn(false);

        boolean result = useCase.detectAndArrive(participant, APPOINTMENT_ID, DEST_LAT, DEST_LON,
                LocalDateTime.now());

        assertThat(result).isFalse();
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    /** ARRIVED 상태 — 조기 반환하므로 member/appointment 스텁 불필요 */
    private AppointmentParticipant mockArrivedParticipant() {
        AppointmentParticipant participant = org.mockito.Mockito.mock(AppointmentParticipant.class);
        when(participant.getDepartureStatus()).thenReturn(DepartureStatus.ARRIVED);
        return participant;
    }

    /** DEPARTED 상태 — member/appointment 스텁 포함 */
    private AppointmentParticipant mockParticipant(DepartureStatus status) {
        Member member = org.mockito.Mockito.mock(Member.class);
        when(member.getId()).thenReturn(MEMBER_ID);

        Appointment appointment = org.mockito.Mockito.mock(Appointment.class);
        when(appointment.getDestinationLatitude()).thenReturn(DEST_LAT);
        when(appointment.getDestinationLongitude()).thenReturn(DEST_LON);

        AppointmentParticipant participant = org.mockito.Mockito.mock(AppointmentParticipant.class);
        when(participant.getDepartureStatus()).thenReturn(status);
        when(participant.getMember()).thenReturn(member);
        when(participant.getAppointment()).thenReturn(appointment);
        return participant;
    }
}
