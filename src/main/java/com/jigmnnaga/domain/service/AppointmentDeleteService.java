
package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.repository.AppointmentRepository;
import com.jigmnnaga.global.cache.InviteCodeCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class AppointmentDeleteService {

    private final AppointmentRepository appointmentRepository;
    private final InviteCodeCache inviteCodeCache;

    public void delete(Appointment appointment) {
        String inviteCode = appointment.getInviteCode();

        appointmentRepository.delete(appointment);

        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            inviteCodeCache.evict(inviteCode);
                        }
                    }
            );
        } else {
            inviteCodeCache.evict(inviteCode);
        }
    }
}