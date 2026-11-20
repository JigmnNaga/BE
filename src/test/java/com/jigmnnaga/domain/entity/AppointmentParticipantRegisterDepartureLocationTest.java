package com.jigmnnaga.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppointmentParticipantRegisterDepartureLocationTest {

    private static final BigDecimal LAT = new BigDecimal("37.4979420");
    private static final BigDecimal LON = new BigDecimal("127.0276210");
    private static final BigDecimal LAT_OTHER = new BigDecimal("37.5665350");
    private static final BigDecimal LON_OTHER = new BigDecimal("126.9779692");

    private AppointmentParticipant participant() {
        return AppointmentParticipant.builder()
                .member(mock(Member.class))
                .role(ParticipantRole.MEMBER)
                .status(ParticipationStatus.ACCEPTED)
                .build();
    }

    // ─── 최초 등록 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("최초 출발 위치 등록 시 이동 정보가 없으므로 초기화 대상이 없다")
    void firstRegistration_noTravelInfoToClear() {
        AppointmentParticipant participant = participant();

        participant.registerDepartureLocation("강남역", LAT, LON);

        assertThat(participant.getDepartureLatitude()).isEqualByComparingTo(LAT);
        assertThat(participant.getDepartureLongitude()).isEqualByComparingTo(LON);
        assertThat(participant.getTravelMode()).isNull();
        assertThat(participant.getTravelDurationMinutes()).isNull();
    }

    @Test
    @DisplayName("최초 등록 후 이동수단을 선택하면 travelMode와 travelDurationMinutes가 저장된다")
    void firstRegistration_thenSelectTravelMode_travelInfoSet() {
        AppointmentParticipant participant = participant();
        participant.registerDepartureLocation("강남역", LAT, LON);

        participant.selectTravelMode(TravelMode.SUBWAY, 30, LocalDateTime.now());

        assertThat(participant.getTravelMode()).isEqualTo(TravelMode.SUBWAY);
        assertThat(participant.getTravelDurationMinutes()).isEqualTo(30);
    }

    // ─── 동일 좌표 재등록 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("동일 좌표로 재등록하면 travelMode와 travelDurationMinutes를 초기화하지 않는다")
    void sameCoordinates_travelInfoPreserved() {
        AppointmentParticipant participant = participant();
        participant.registerDepartureLocation("강남역", LAT, LON);
        participant.selectTravelMode(TravelMode.BUS, 20, LocalDateTime.now());

        participant.registerDepartureLocation("강남역 (재등록)", LAT, LON);

        assertThat(participant.getTravelMode()).isEqualTo(TravelMode.BUS);
        assertThat(participant.getTravelDurationMinutes()).isEqualTo(20);
    }

    @Test
    @DisplayName("scale이 다른 BigDecimal이라도 값이 같으면 동일 좌표로 판단해 이동 정보를 유지한다")
    void sameValueDifferentScale_treatedAsSameCoordinate() {
        AppointmentParticipant participant = participant();
        participant.registerDepartureLocation("강남역", LAT, LON);
        participant.selectTravelMode(TravelMode.TAXI, 15, LocalDateTime.now());

        // 소수점 자리수(scale)만 다른 동일한 값
        BigDecimal sameLatDifferentScale = new BigDecimal("37.49794200000");
        BigDecimal sameLonDifferentScale = new BigDecimal("127.02762100000");
        participant.registerDepartureLocation("강남역 (scale 다름)", sameLatDifferentScale, sameLonDifferentScale);

        assertThat(participant.getTravelMode()).isEqualTo(TravelMode.TAXI);
        assertThat(participant.getTravelDurationMinutes()).isEqualTo(15);
    }

    // ─── 다른 좌표 등록 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("실제로 다른 좌표를 등록하면 travelMode를 null로 초기화한다")
    void differentCoordinates_travelModeCleared() {
        AppointmentParticipant participant = participant();
        participant.registerDepartureLocation("강남역", LAT, LON);
        participant.selectTravelMode(TravelMode.SUBWAY, 30, LocalDateTime.now());

        participant.registerDepartureLocation("서울시청", LAT_OTHER, LON_OTHER);

        assertThat(participant.getTravelMode()).isNull();
    }

    @Test
    @DisplayName("실제로 다른 좌표를 등록하면 travelDurationMinutes를 null로 초기화한다")
    void differentCoordinates_travelDurationCleared() {
        AppointmentParticipant participant = participant();
        participant.registerDepartureLocation("강남역", LAT, LON);
        participant.selectTravelMode(TravelMode.BUS, 25, LocalDateTime.now());

        participant.registerDepartureLocation("서울시청", LAT_OTHER, LON_OTHER);

        assertThat(participant.getTravelDurationMinutes()).isNull();
    }

    @Test
    @DisplayName("위도만 다르면 좌표가 변경된 것으로 판단해 이동 정보를 초기화한다")
    void latitudeOnlyChanged_travelInfoCleared() {
        AppointmentParticipant participant = participant();
        participant.registerDepartureLocation("강남역", LAT, LON);
        participant.selectTravelMode(TravelMode.TAXI, 10, LocalDateTime.now());

        participant.registerDepartureLocation("위도만 변경", LAT_OTHER, LON);

        assertThat(participant.getTravelMode()).isNull();
        assertThat(participant.getTravelDurationMinutes()).isNull();
    }

    @Test
    @DisplayName("경도만 다르면 좌표가 변경된 것으로 판단해 이동 정보를 초기화한다")
    void longitudeOnlyChanged_travelInfoCleared() {
        AppointmentParticipant participant = participant();
        participant.registerDepartureLocation("강남역", LAT, LON);
        participant.selectTravelMode(TravelMode.BUS, 20, LocalDateTime.now());

        participant.registerDepartureLocation("경도만 변경", LAT, LON_OTHER);

        assertThat(participant.getTravelMode()).isNull();
        assertThat(participant.getTravelDurationMinutes()).isNull();
    }

    // ─── 출발 상태 검증 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("DEPARTED 상태에서 출발 위치 변경을 시도하면 DEPARTURE_LOCATION_LOCKED 예외가 발생한다")
    void whenDeparted_throwsDepartureLocationLocked() {
        AppointmentParticipant participant = participant();
        participant.depart();

        assertThatThrownBy(() -> participant.registerDepartureLocation("서울시청", LAT_OTHER, LON_OTHER))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTURE_LOCATION_LOCKED);
    }

    @Test
    @DisplayName("ARRIVED 상태에서 출발 위치 변경을 시도하면 DEPARTURE_LOCATION_LOCKED 예외가 발생한다")
    void whenArrived_throwsDepartureLocationLocked() {
        AppointmentParticipant participant = participant();
        participant.depart();
        participant.arrive(LocalDateTime.now());

        assertThatThrownBy(() -> participant.registerDepartureLocation("서울시청", LAT_OTHER, LON_OTHER))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTURE_LOCATION_LOCKED);
    }

    @Test
    @DisplayName("BEFORE_DEPARTURE 상태에서는 출발 위치를 정상적으로 등록할 수 있다")
    void beforeDeparture_registrationSucceeds() {
        AppointmentParticipant participant = participant();

        participant.registerDepartureLocation("강남역", LAT, LON);

        assertThat(participant.getDepartureLocationName()).isEqualTo("강남역");
        assertThat(participant.getDepartureLatitude()).isEqualByComparingTo(LAT);
        assertThat(participant.getDepartureLongitude()).isEqualByComparingTo(LON);
    }
}
