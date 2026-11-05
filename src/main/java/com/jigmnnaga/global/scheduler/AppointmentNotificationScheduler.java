package com.jigmnnaga.global.scheduler;

import com.jigmnnaga.application.usecase.ScheduledNotificationUseCase;
import com.jigmnnaga.global.util.TimeZones;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class AppointmentNotificationScheduler {

    private final ScheduledNotificationUseCase scheduledNotificationUseCase;

    @Scheduled(fixedDelayString = "${notification.scheduler.fixed-delay-ms:60000}")
    public void sendScheduledNotifications() {
        LocalDateTime now = LocalDateTime.now(TimeZones.SEOUL);
        run("투표 마감 임박", () -> scheduledNotificationUseCase.sendDeadlineReminders(now));
        run("응답 기한 마감", () -> scheduledNotificationUseCase.sendDeadlineClosedNotifications(now));
        run("약속 5시간 전", () -> scheduledNotificationUseCase.sendPreAppointmentNotifications(now));
    }

    private void run(String name, NotificationJob job) {
        try {
            int processedCount = job.run();
            if (processedCount > 0) {
                log.info("예약 알림 처리 완료 job={}, appointments={}", name, processedCount);
            }
        } catch (RuntimeException e) {
            log.error("예약 알림 처리 실패 job={}", name, e);
        }
    }

    @FunctionalInterface
    private interface NotificationJob {
        int run();
    }
}
