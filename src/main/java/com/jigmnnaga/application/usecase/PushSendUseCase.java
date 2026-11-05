package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.event.PushEvent;
import com.jigmnnaga.domain.service.DeviceTokenDeleteService;
import com.jigmnnaga.domain.service.DeviceTokenGetService;
import com.jigmnnaga.global.client.fcm.FcmClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushSendUseCase {

    private final FcmClient fcmClient;
    private final DeviceTokenGetService deviceTokenGetService;
    private final DeviceTokenDeleteService deviceTokenDeleteService;

    @Async("pushExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendPush(PushEvent event) {
        if (!fcmClient.isEnabled() || event.recipientIds().isEmpty()) {
            return;
        }

        List<String> tokens = deviceTokenGetService.findAllTokensByMemberIds(event.recipientIds());
        List<String> invalidTokens = fcmClient.send(tokens, event.title(), event.body(), event.data(), false);
        if (!invalidTokens.isEmpty()) {
            deviceTokenDeleteService.deleteAllByTokens(invalidTokens);
            log.info("만료되거나 잘못된 기기 토큰 삭제 count={}", invalidTokens.size());
        }
    }
}
