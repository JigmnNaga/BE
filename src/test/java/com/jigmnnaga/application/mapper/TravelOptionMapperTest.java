package com.jigmnnaga.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jigmnnaga.application.dto.response.TravelOptionsResponse;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.TravelMode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TravelOptionMapperTest {

    private final TravelOptionMapper mapper = new TravelOptionMapper();

    private Appointment appointment(LocalDateTime confirmedDateTime) {
        Appointment appointment = mock(Appointment.class);
        when(appointment.getConfirmedDateTime()).thenReturn(confirmedDateTime);
        when(appointment.getDestinationName()).thenReturn("강남역");
        when(appointment.getDestinationLatitude()).thenReturn(37.4979);
        when(appointment.getDestinationLongitude()).thenReturn(127.0276);
        return appointment;
    }

    private AppointmentParticipant me(TravelMode mode, Integer durationMinutes) {
        AppointmentParticipant me = mock(AppointmentParticipant.class);
        when(me.getTravelMode()).thenReturn(mode);
        when(me.getTravelDurationMinutes()).thenReturn(durationMinutes);
        return me;
    }

    @Test
    @DisplayName("약속 시각과 선택한 이동수단의 예상 이동시간으로 출발 예정 시각을 계산한다")
    void calculateDepartureTime() {
        Appointment appointment = appointment(LocalDateTime.of(2026, 11, 15, 15, 0));
        AppointmentParticipant me = me(TravelMode.BUS, 45);
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 12, 0);

        TravelOptionsResponse response = mapper.toTravelOptionsResponse(appointment, me, List.of(), now);

        assertThat(response.departureTime()).isEqualTo(LocalDateTime.of(2026, 11, 15, 14, 15));
    }

    @Test
    @DisplayName("날짜가 바뀌는 이동시간도 올바르게 계산한다")
    void calculateDepartureTimeAcrossMidnight() {
        Appointment appointment = appointment(LocalDateTime.of(2026, 11, 15, 0, 30));
        AppointmentParticipant me = me(TravelMode.TAXI, 60);
        LocalDateTime now = LocalDateTime.of(2026, 11, 14, 22, 0);

        TravelOptionsResponse response = mapper.toTravelOptionsResponse(appointment, me, List.of(), now);

        assertThat(response.departureTime()).isEqualTo(LocalDateTime.of(2026, 11, 14, 23, 30));
    }

    @Test
    @DisplayName("이동수단이 미선택이면 출발 예정 시각은 null이다")
    void departureTimeNullWhenTravelModeNotSelected() {
        Appointment appointment = appointment(LocalDateTime.of(2026, 11, 15, 15, 0));
        AppointmentParticipant me = me(null, null);
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 12, 0);

        TravelOptionsResponse response = mapper.toTravelOptionsResponse(appointment, me, List.of(), now);

        assertThat(response.departureTime()).isNull();
    }

    @Test
    @DisplayName("예상 이동시간이 미설정이면 출발 예정 시각은 null이다")
    void departureTimeNullWhenDurationNotSet() {
        Appointment appointment = appointment(LocalDateTime.of(2026, 11, 15, 15, 0));
        AppointmentParticipant me = me(TravelMode.BUS, null);
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 12, 0);

        TravelOptionsResponse response = mapper.toTravelOptionsResponse(appointment, me, List.of(), now);

        assertThat(response.departureTime()).isNull();
    }

    @Test
    @DisplayName("예상 이동시간이 0분이면 비정상 데이터로 보아 출발 예정 시각은 null이다")
    void departureTimeNullWhenDurationIsZero() {
        Appointment appointment = appointment(LocalDateTime.of(2026, 11, 15, 15, 0));
        AppointmentParticipant me = me(TravelMode.BUS, 0);
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 12, 0);

        TravelOptionsResponse response = mapper.toTravelOptionsResponse(appointment, me, List.of(), now);

        assertThat(response.departureTime()).isNull();
    }

    @Test
    @DisplayName("약속 확정 시각이 없으면 출발 예정 시각은 null이다")
    void departureTimeNullWhenAppointmentDateTimeNotConfirmed() {
        Appointment appointment = appointment(null);
        AppointmentParticipant me = me(TravelMode.BUS, 45);
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 12, 0);

        TravelOptionsResponse response = mapper.toTravelOptionsResponse(appointment, me, List.of(), now);

        assertThat(response.departureTime()).isNull();
    }

    @Test
    @DisplayName("기존 응답 필드가 모두 유지된다")
    void existingFieldsArePreserved() {
        LocalDateTime appointmentDateTime = LocalDateTime.of(2026, 11, 15, 15, 0);
        Appointment appointment = appointment(appointmentDateTime);
        AppointmentParticipant me = me(TravelMode.TAXI, 30);
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 12, 0);

        TravelOptionsResponse response = mapper.toTravelOptionsResponse(appointment, me, List.of(), now);

        assertThat(response.destinationName()).isEqualTo("강남역");
        assertThat(response.destinationLatitude()).isEqualTo(37.4979);
        assertThat(response.destinationLongitude()).isEqualTo(127.0276);
        assertThat(response.appointmentDateTime()).isEqualTo(appointmentDateTime);
        assertThat(response.myTravelMode()).isEqualTo(TravelMode.TAXI);
        assertThat(response.options()).isEmpty();
    }

    @Test
    @DisplayName("현재 시각이 출발 예정 시각 이후이면 출발 필요 여부는 true이다")
    void departureRequiredTrueWhenCurrentTimeIsAfterDepartureTime() {
        Appointment appointment = appointment(LocalDateTime.of(2026, 11, 15, 15, 0));
        AppointmentParticipant me = me(TravelMode.BUS, 45);
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 14, 30);

        TravelOptionsResponse response = mapper.toTravelOptionsResponse(appointment, me, List.of(), now);

        assertThat(response.departureRequired()).isTrue();
    }

    @Test
    @DisplayName("현재 시각이 출발 예정 시각과 정확히 같으면 출발 필요 여부는 true이다")
    void departureRequiredTrueWhenCurrentTimeEqualsExactlyDepartureTime() {
        Appointment appointment = appointment(LocalDateTime.of(2026, 11, 15, 15, 0));
        AppointmentParticipant me = me(TravelMode.BUS, 45);
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 14, 15);

        TravelOptionsResponse response = mapper.toTravelOptionsResponse(appointment, me, List.of(), now);

        assertThat(response.departureRequired()).isTrue();
    }

    @Test
    @DisplayName("현재 시각이 출발 예정 시각 이전이면 출발 필요 여부는 false이다")
    void departureRequiredFalseWhenCurrentTimeIsBeforeDepartureTime() {
        Appointment appointment = appointment(LocalDateTime.of(2026, 11, 15, 15, 0));
        AppointmentParticipant me = me(TravelMode.BUS, 45);
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 14, 0);

        TravelOptionsResponse response = mapper.toTravelOptionsResponse(appointment, me, List.of(), now);

        assertThat(response.departureRequired()).isFalse();
    }

    @Test
    @DisplayName("출발 예정 시각을 계산할 수 없으면 출발 필요 여부는 null이다")
    void departureRequiredNullWhenDepartureTimeCannotBeCalculated() {
        Appointment appointment = appointment(LocalDateTime.of(2026, 11, 15, 15, 0));
        AppointmentParticipant me = me(null, null);
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 14, 0);

        TravelOptionsResponse response = mapper.toTravelOptionsResponse(appointment, me, List.of(), now);

        assertThat(response.departureRequired()).isNull();
    }
}
