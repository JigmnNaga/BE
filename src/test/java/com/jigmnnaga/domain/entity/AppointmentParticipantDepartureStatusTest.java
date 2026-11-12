package com.jigmnnaga.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppointmentParticipantDepartureStatusTest {

    private AppointmentParticipant participant() {
        return AppointmentParticipant.builder()
                .member(mock(Member.class))
                .role(ParticipantRole.MEMBER)
                .status(ParticipationStatus.ACCEPTED)
                .build();
    }

    @Test
    @DisplayName("참여자 생성 시 출발 상태의 초기값은 BEFORE_DEPARTURE이다")
    void initialStatusIsBeforeDeparture() {
        AppointmentParticipant participant = participant();

        assertThat(participant.getDepartureStatus()).isEqualTo(DepartureStatus.BEFORE_DEPARTURE);
    }

    @Test
    @DisplayName("BEFORE_DEPARTURE 상태에서 depart()를 호출하면 DEPARTED로 전이된다")
    void departFromBeforeDeparture() {
        AppointmentParticipant participant = participant();

        participant.depart();

        assertThat(participant.getDepartureStatus()).isEqualTo(DepartureStatus.DEPARTED);
    }

    @Test
    @DisplayName("DEPARTED 상태에서 arrive()를 호출하면 ARRIVED로 전이된다")
    void arriveFromDeparted() {
        AppointmentParticipant participant = participant();
        participant.depart();

        participant.arrive();

        assertThat(participant.getDepartureStatus()).isEqualTo(DepartureStatus.ARRIVED);
    }

    @Test
    @DisplayName("DEPARTED 상태에서 depart()를 다시 호출하면 ALREADY_DEPARTED 예외가 발생한다")
    void departAgainWhenAlreadyDeparted() {
        AppointmentParticipant participant = participant();
        participant.depart();

        assertThatThrownBy(participant::depart)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_DEPARTED);
    }

    @Test
    @DisplayName("ARRIVED 상태에서 arrive()를 다시 호출하면 ALREADY_ARRIVED 예외가 발생한다")
    void arriveAgainWhenAlreadyArrived() {
        AppointmentParticipant participant = participant();
        participant.depart();
        participant.arrive();

        assertThatThrownBy(participant::arrive)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_ARRIVED);
    }

    @Test
    @DisplayName("BEFORE_DEPARTURE 상태에서 arrive()를 호출하면 NOT_YET_DEPARTED 예외가 발생한다")
    void arriveBeforeDeparting() {
        AppointmentParticipant participant = participant();

        assertThatThrownBy(participant::arrive)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_YET_DEPARTED);
    }

    @Test
    @DisplayName("ARRIVED 상태에서 depart()를 호출하면 ALREADY_ARRIVED 예외가 발생한다")
    void departAfterArrived() {
        AppointmentParticipant participant = participant();
        participant.depart();
        participant.arrive();

        assertThatThrownBy(participant::depart)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_ARRIVED);
    }

    @Test
    @DisplayName("기존 ParticipationStatus 및 AttendanceStatus 동작에 영향을 주지 않는다")
    void existingStatusFieldsAreUnaffected() {
        AppointmentParticipant participant = participant();

        participant.depart();
        participant.arrive();

        assertThat(participant.getStatus()).isEqualTo(ParticipationStatus.ACCEPTED);
        assertThat(participant.getAttendanceStatus()).isEqualTo(AttendanceStatus.PENDING);
    }
}
