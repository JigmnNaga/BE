
package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.repository.AppointmentRepository;
import com.jigmnnaga.global.cache.InviteCodeCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentGetServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private InviteCodeCache inviteCodeCache;

    @InjectMocks
    private AppointmentGetService appointmentGetService;

    @Test
    void 캐시가_없으면_DB에서_조회하고_Redis에_저장한다() {
        String inviteCode = "ABC12345";
        Appointment appointment = mock(Appointment.class);

        when(inviteCodeCache.get(inviteCode))
                .thenReturn(Optional.empty());
        when(appointmentRepository.findByInviteCode(inviteCode))
                .thenReturn(Optional.of(appointment));
        when(appointment.getId()).thenReturn(1L);

        Optional<Appointment> result =
                appointmentGetService.findByInviteCode(inviteCode);

        assertThat(result).contains(appointment);
        verify(inviteCodeCache).put(inviteCode, 1L);
    }

    @Test
    void 캐시가_있으면_약속_ID로_조회한다() {
        String inviteCode = "ABC12345";
        Appointment appointment = mock(Appointment.class);

        when(inviteCodeCache.get(inviteCode))
                .thenReturn(Optional.of(1L));
        when(appointmentRepository.findById(1L))
                .thenReturn(Optional.of(appointment));
        when(appointment.getInviteCode()).thenReturn(inviteCode);

        Optional<Appointment> result =
                appointmentGetService.findByInviteCode(inviteCode);

        assertThat(result).contains(appointment);
        verify(appointmentRepository, never()).findByInviteCode(inviteCode);
    }

    @Test
    void 캐시된_약속이_없으면_캐시를_삭제하고_DB에서_재조회한다() {
        String inviteCode = "ABC12345";

        when(inviteCodeCache.get(inviteCode))
                .thenReturn(Optional.of(1L));
        when(appointmentRepository.findById(1L))
                .thenReturn(Optional.empty());
        when(appointmentRepository.findByInviteCode(inviteCode))
                .thenReturn(Optional.empty());

        Optional<Appointment> result =
                appointmentGetService.findByInviteCode(inviteCode);

        assertThat(result).isEmpty();
        verify(inviteCodeCache).evict(inviteCode);
        verify(appointmentRepository).findByInviteCode(inviteCode);
    }
}
