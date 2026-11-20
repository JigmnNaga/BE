package com.jigmnnaga.application.usecase;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jigmnnaga.application.dto.request.ConfirmDateTimeRequest;
import com.jigmnnaga.application.event.AppointmentEventFactory;
import com.jigmnnaga.application.event.AppointmentEventPublisher;
import com.jigmnnaga.application.validator.AppointmentAccessValidator;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentCandidateDate;
import com.jigmnnaga.global.cache.EtaCache;
import com.jigmnnaga.global.cache.LatenessCache;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentConfirmUseCaseTest {

    @Mock
    private AppointmentAccessValidator appointmentAccessValidator;

    @Mock
    private AppointmentEventPublisher appointmentEventPublisher;

    @Mock
    private AppointmentEventFactory appointmentEventFactory;

    @Mock
    private EtaCache etaCache;

    @Mock
    private LatenessCache latenessCache;

    @InjectMocks
    private AppointmentConfirmUseCase useCase;

    private static final Long MEMBER_ID = 1L;
    private static final Long APPOINTMENT_ID = 2L;

    private Appointment mockAppointment(LocalDate confirmedDate, LocalTime confirmedTime,
                                        LocalDate candidateDate) {
        Appointment appointment = Mockito.mock(Appointment.class);
        Mockito.lenient().when(appointment.getConfirmedDate()).thenReturn(confirmedDate);
        Mockito.lenient().when(appointment.getConfirmedTime()).thenReturn(confirmedTime);
        Mockito.lenient().when(appointment.getParticipants()).thenReturn(Collections.emptyList());
        Mockito.lenient().when(appointment.getAcceptedMemberIdsExcept(anyLong())).thenReturn(Collections.emptyList());
        Mockito.lenient().when(appointment.isConfirmed()).thenReturn(false);

        AppointmentCandidateDate candidate = Mockito.mock(AppointmentCandidateDate.class);
        Mockito.lenient().when(candidate.getCandidateDate()).thenReturn(candidateDate);
        Mockito.lenient().when(appointment.getCandidateDates()).thenReturn(List.of(candidate));

        return appointment;
    }

    @Test
    @DisplayName("약속 날짜/시각이 변경되면 ETA·지각 캐시를 무효화한다")
    void confirmDateTime_dateTimeChanged_evictsCaches() {
        LocalDate oldDate = LocalDate.of(2026, 11, 20);
        LocalTime oldTime = LocalTime.of(14, 0);
        LocalDate newDate = LocalDate.of(2026, 11, 21);
        LocalTime newTime = LocalTime.of(15, 0);

        Appointment appointment = mockAppointment(oldDate, oldTime, newDate);
        when(appointmentAccessValidator.getHostAppointment(MEMBER_ID, APPOINTMENT_ID))
                .thenReturn(appointment);

        useCase.confirmDateTime(MEMBER_ID, APPOINTMENT_ID, new ConfirmDateTimeRequest(newDate, newTime));

        verify(etaCache).evictResultsByAppointment(APPOINTMENT_ID);
        verify(latenessCache).evictByAppointment(APPOINTMENT_ID);
    }

    @Test
    @DisplayName("날짜만 변경되면 ETA·지각 캐시를 무효화한다")
    void confirmDateTime_onlyDateChanged_evictsCaches() {
        LocalDate oldDate = LocalDate.of(2026, 11, 20);
        LocalTime sameTime = LocalTime.of(14, 0);
        LocalDate newDate = LocalDate.of(2026, 11, 21);

        Appointment appointment = mockAppointment(oldDate, sameTime, newDate);
        when(appointmentAccessValidator.getHostAppointment(MEMBER_ID, APPOINTMENT_ID))
                .thenReturn(appointment);

        useCase.confirmDateTime(MEMBER_ID, APPOINTMENT_ID, new ConfirmDateTimeRequest(newDate, sameTime));

        verify(etaCache).evictResultsByAppointment(APPOINTMENT_ID);
        verify(latenessCache).evictByAppointment(APPOINTMENT_ID);
    }

    @Test
    @DisplayName("시각만 변경되면 ETA·지각 캐시를 무효화한다")
    void confirmDateTime_onlyTimeChanged_evictsCaches() {
        LocalDate sameDate = LocalDate.of(2026, 11, 20);
        LocalTime oldTime = LocalTime.of(14, 0);
        LocalTime newTime = LocalTime.of(16, 0);

        Appointment appointment = mockAppointment(sameDate, oldTime, sameDate);
        when(appointmentAccessValidator.getHostAppointment(MEMBER_ID, APPOINTMENT_ID))
                .thenReturn(appointment);

        useCase.confirmDateTime(MEMBER_ID, APPOINTMENT_ID, new ConfirmDateTimeRequest(sameDate, newTime));

        verify(etaCache).evictResultsByAppointment(APPOINTMENT_ID);
        verify(latenessCache).evictByAppointment(APPOINTMENT_ID);
    }

    @Test
    @DisplayName("날짜/시각이 동일하면 ETA·지각 캐시를 무효화하지 않는다")
    void confirmDateTime_sameDateTimeReconfirmed_doesNotEvictCaches() {
        LocalDate sameDate = LocalDate.of(2026, 11, 20);
        LocalTime sameTime = LocalTime.of(14, 0);

        Appointment appointment = mockAppointment(sameDate, sameTime, sameDate);
        when(appointmentAccessValidator.getHostAppointment(MEMBER_ID, APPOINTMENT_ID))
                .thenReturn(appointment);

        useCase.confirmDateTime(MEMBER_ID, APPOINTMENT_ID, new ConfirmDateTimeRequest(sameDate, sameTime));

        verify(etaCache, never()).evictResultsByAppointment(anyLong());
        verify(latenessCache, never()).evictByAppointment(anyLong());
    }

    @Test
    @DisplayName("최초 확정 시 (기존 null) ETA·지각 캐시를 무효화한다")
    void confirmDateTime_firstConfirmation_evictsCaches() {
        LocalDate newDate = LocalDate.of(2026, 11, 20);
        LocalTime newTime = LocalTime.of(14, 0);

        // confirmedDate/Time이 null — 최초 확정
        Appointment appointment = mockAppointment(null, null, newDate);
        when(appointmentAccessValidator.getHostAppointment(MEMBER_ID, APPOINTMENT_ID))
                .thenReturn(appointment);

        useCase.confirmDateTime(MEMBER_ID, APPOINTMENT_ID, new ConfirmDateTimeRequest(newDate, newTime));

        verify(etaCache).evictResultsByAppointment(APPOINTMENT_ID);
        verify(latenessCache).evictByAppointment(APPOINTMENT_ID);
    }
}
