package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.event.ProfileImageDiscardedEvent;
import com.jigmnnaga.global.client.s3.S3FileClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ProfileImageCleanupUseCase {

    private final S3FileClient s3FileClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deleteDiscardedImage(ProfileImageDiscardedEvent event) {
        s3FileClient.delete(event.profileImageKey());
    }
}
