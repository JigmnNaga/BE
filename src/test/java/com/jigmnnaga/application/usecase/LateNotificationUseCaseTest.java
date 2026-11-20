package com.jigmnnaga.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jigmnnaga.application.event.AppointmentEventFactory;
import com.jigmnnaga.application.event.AppointmentEventPublisher;
import com.jigmnnaga.application.event.NotificationEvent;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AttendanceStatus;
import com.jigmnnaga.domain.entity.DepartureStatus;
import com.jigmnnaga.domain.entity.NotificationType;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import com.jigmnnaga.domain.repository.AppointmentParticipantRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LateNotificationUseCaseTest {

    @Mock
    private AppointmentParticipantRepository appointmentParticipantRepository;

    @Mock
    private AppointmentEventPublisher appointmentEventPublisher;

    @Mock
    private AppointmentEventFactory appointmentEventFactory;

    @InjectMocks
    private LateNotificationUseCase lateNotificationUseCase;

    private static final Long APPOINTMENT_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final int LATENESS_MINUTES = 15;

    private Appointment mockAppointment(boolean canceled, boolean completed, LocalDateTime confirmedDateTime) {
        Appointment appointment = org.mockito.Mockito.mock(Appointment.class);
        org.mockito.Mockito.lenient().when(appointment.isCanceled()).thenReturn(canceled);
        org.mockito.Mockito.lenient().when(appointment.getCompletedAt())
                .thenReturn(completed ? LocalDateTime.now().minusMinutes(10) : null);
        org.mockito.Mockito.lenient().when(appointment.getConfirmedDateTime()).thenReturn(confirmedDateTime);
        return appointment;
    }

    @Test
    @DisplayName("markLateNotifSentIfNotYet가 0을 반환하면 (이미 발송됨) 알림을 발행하지 않는다")
    void sendIfNeeded_whenAlreadySent_skips() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime confirmedAt = now.plusMinutes(10);
        Appointment appointment = mockAppointment(false, false, confirmedAt);

        when(appointmentParticipantRepository.markLateNotifSentIfNotYet(
                eq(APPOINTMENT_ID), eq(MEMBER_ID), any(),
                eq(ParticipationStatus.ACCEPTED), eq(AttendanceStatus.ATTENDING), eq(DepartureStatus.DEPARTED)))
                .thenReturn(0);

        lateNotificationUseCase.sendIfNeeded(MEMBER_ID, APPOINTMENT_ID, appointment, LATENESS_MINUTES, now);

        verify(appointmentEventPublisher, never()).publishNotification(any());
    }

    @Test
    @DisplayName("취소된 약속이면 조건부 UPDATE 없이 바로 알림을 발행하지 않는다")
    void sendIfNeeded_whenAppointmentCanceled_skips() {
        LocalDateTime now = LocalDateTime.now();
        Appointment appointment = mockAppointment(true, false, now.plusMinutes(10));

        lateNotificationUseCase.sendIfNeeded(MEMBER_ID, APPOINTMENT_ID, appointment, LATENESS_MINUTES, now);

        verify(appointmentParticipantRepository, never()).markLateNotifSentIfNotYet(
                anyLong(), anyLong(), any(), any(), any(), any());
        verify(appointmentEventPublisher, never()).publishNotification(any());
    }

    @Test
    @DisplayName("완료된 약속이면 조건부 UPDATE 없이 바로 알림을 발행하지 않는다")
    void sendIfNeeded_whenAppointmentCompleted_skips() {
        LocalDateTime now = LocalDateTime.now();
        Appointment appointment = mockAppointment(false, true, now.plusMinutes(10));

        lateNotificationUseCase.sendIfNeeded(MEMBER_ID, APPOINTMENT_ID, appointment, LATENESS_MINUTES, now);

        verify(appointmentParticipantRepository, never()).markLateNotifSentIfNotYet(
                anyLong(), anyLong(), any(), any(), any(), any());
        verify(appointmentEventPublisher, never()).publishNotification(any());
    }

    @Test
    @DisplayName("약속 시각이 이미 지났으면 알림을 발행하지 않는다")
    void sendIfNeeded_whenAppointmentTimePassed_skips() {
        LocalDateTime now = LocalDateTime.now();
        // confirmedDateTime == now (not strictly before)
        Appointment appointment = mockAppointment(false, false, now);

        lateNotificationUseCase.sendIfNeeded(MEMBER_ID, APPOINTMENT_ID, appointment, LATENESS_MINUTES, now);

        verify(appointmentParticipantRepository, never()).markLateNotifSentIfNotYet(
                anyLong(), anyLong(), any(), any(), any(), any());
        verify(appointmentEventPublisher, never()).publishNotification(any());
    }

    @Test
    @DisplayName("confirmedDateTime이 null이면 (미확정 약속) 알림을 발행하지 않는다")
    void sendIfNeeded_whenNotConfirmed_skips() {
        LocalDateTime now = LocalDateTime.now();
        Appointment appointment = mockAppointment(false, false, null);

        lateNotificationUseCase.sendIfNeeded(MEMBER_ID, APPOINTMENT_ID, appointment, LATENESS_MINUTES, now);

        verify(appointmentParticipantRepository, never()).markLateNotifSentIfNotYet(
                anyLong(), anyLong(), any(), any(), any(), any());
        verify(appointmentEventPublisher, never()).publishNotification(any());
    }

    @Test
    @DisplayName("모든 조건을 충족하면 알림을 1회 발행한다")
    void sendIfNeeded_whenEligible_publishesNotification() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime confirmedAt = now.plusMinutes(10);
        Appointment appointment = mockAppointment(false, false, confirmedAt);

        NotificationEvent expectedEvent = new NotificationEvent(
                List.of(MEMBER_ID), APPOINTMENT_ID, NotificationType.LATE_EXPECTED,
                "지각이 예상돼요", "약 15분 늦을 것으로 예상돼요.");

        when(appointmentParticipantRepository.markLateNotifSentIfNotYet(
                eq(APPOINTMENT_ID), eq(MEMBER_ID), any(),
                eq(ParticipationStatus.ACCEPTED), eq(AttendanceStatus.ATTENDING), eq(DepartureStatus.DEPARTED)))
                .thenReturn(1);
        when(appointmentEventFactory.lateExpected(eq(appointment), eq(LATENESS_MINUTES), eq(MEMBER_ID)))
                .thenReturn(expectedEvent);

        lateNotificationUseCase.sendIfNeeded(MEMBER_ID, APPOINTMENT_ID, appointment, LATENESS_MINUTES, now);

        verify(appointmentEventPublisher, times(1)).publishNotification(eq(expectedEvent));
    }

    @Test
    @DisplayName("ACCEPTED+ATTENDING+DEPARTED 조건 불충족 시 markLateNotifSentIfNotYet가 0을 반환하면 알림을 발행하지 않는다")
    void sendIfNeeded_whenNotEligibleStatus_skips() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime confirmedAt = now.plusMinutes(10);
        Appointment appointment = mockAppointment(false, false, confirmedAt);

        // 상태 조건 불충족: ACCEPTED+ATTENDING+DEPARTED 아님 → DB UPDATE 조건에서 0 반환
        when(appointmentParticipantRepository.markLateNotifSentIfNotYet(
                eq(APPOINTMENT_ID), eq(MEMBER_ID), any(),
                eq(ParticipationStatus.ACCEPTED), eq(AttendanceStatus.ATTENDING), eq(DepartureStatus.DEPARTED)))
                .thenReturn(0);

        lateNotificationUseCase.sendIfNeeded(MEMBER_ID, APPOINTMENT_ID, appointment, LATENESS_MINUTES, now);

        verify(appointmentEventPublisher, never()).publishNotification(any());
    }

    @Test
    @DisplayName("동시 호출 시 markLateNotifSentIfNotYet 첫 번째만 1을 반환해 알림이 1번만 발행된다")
    void sendIfNeeded_concurrentCalls_onlyOncePublished() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime confirmedAt = now.plusMinutes(10);
        Appointment appointment = mockAppointment(false, false, confirmedAt);

        NotificationEvent expectedEvent = new NotificationEvent(
                List.of(MEMBER_ID), APPOINTMENT_ID, NotificationType.LATE_EXPECTED,
                "지각이 예상돼요", "약 15분 늦을 것으로 예상돼요.");

        // 첫 번째 호출: 1 (성공), 두 번째 호출: 0 (이미 처리됨)
        when(appointmentParticipantRepository.markLateNotifSentIfNotYet(
                eq(APPOINTMENT_ID), eq(MEMBER_ID), any(),
                eq(ParticipationStatus.ACCEPTED), eq(AttendanceStatus.ATTENDING), eq(DepartureStatus.DEPARTED)))
                .thenReturn(1)
                .thenReturn(0);
        when(appointmentEventFactory.lateExpected(eq(appointment), anyInt(), eq(MEMBER_ID)))
                .thenReturn(expectedEvent);

        lateNotificationUseCase.sendIfNeeded(MEMBER_ID, APPOINTMENT_ID, appointment, LATENESS_MINUTES, now);
        lateNotificationUseCase.sendIfNeeded(MEMBER_ID, APPOINTMENT_ID, appointment, LATENESS_MINUTES, now);

        // 알림은 1번만 발행돼야 함
        verify(appointmentEventPublisher, times(1)).publishNotification(any());
    }
}
