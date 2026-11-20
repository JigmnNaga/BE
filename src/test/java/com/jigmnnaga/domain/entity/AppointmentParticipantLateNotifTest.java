package com.jigmnnaga.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppointmentParticipantLateNotifTest {

    private AppointmentParticipant participant() {
        return AppointmentParticipant.builder()
                .member(mock(Member.class))
                .role(ParticipantRole.MEMBER)
                .status(ParticipationStatus.ACCEPTED)
                .build();
    }

    @Test
    @DisplayName("lateNotifSentAt이 null이면 isLateNotifSent()는 false를 반환한다")
    void isLateNotifSent_whenNull_returnsFalse() {
        AppointmentParticipant participant = participant();

        assertThat(participant.isLateNotifSent()).isFalse();
    }

    @Test
    @DisplayName("markLateNotifSent 호출 후 isLateNotifSent()는 true를 반환한다")
    void markLateNotifSent_setsTimestamp() {
        AppointmentParticipant participant = participant();
        LocalDateTime sentAt = LocalDateTime.now();

        participant.markLateNotifSent(sentAt);

        assertThat(participant.isLateNotifSent()).isTrue();
        assertThat(participant.getLateNotifSentAt()).isEqualTo(sentAt);
    }

    @Test
    @DisplayName("resetLateNotifHistory 호출 후 lateNotifSentAt이 null로 초기화된다")
    void resetLateNotifHistory_clearsTimestamp() {
        AppointmentParticipant participant = participant();
        participant.markLateNotifSent(LocalDateTime.now());

        participant.resetLateNotifHistory();

        assertThat(participant.isLateNotifSent()).isFalse();
        assertThat(participant.getLateNotifSentAt()).isNull();
    }

    @Test
    @DisplayName("약속 시각 변경 시 lateNotifSentAt이 초기화되어 재발송이 가능해진다")
    void confirmDateTime_change_resetsLateNotifHistory() {
        AppointmentParticipant participant = participant();
        participant.markLateNotifSent(LocalDateTime.now());

        // confirmDateTime 변경은 Appointment 도메인 메서드에서 resetLateNotifHistory를 호출한다.
        // 여기서는 직접 메서드를 검증한다.
        participant.resetLateNotifHistory();

        assertThat(participant.isLateNotifSent()).isFalse();
    }
}
