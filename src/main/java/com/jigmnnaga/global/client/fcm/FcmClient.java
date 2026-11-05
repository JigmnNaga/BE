package com.jigmnnaga.global.client.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableConfigurationProperties(FirebaseProperties.class)
public class FcmClient {

    private static final int MAX_TOKENS_PER_REQUEST = 500;
    private static final Set<MessagingErrorCode> INVALID_TOKEN_ERRORS =
            Set.of(MessagingErrorCode.UNREGISTERED, MessagingErrorCode.INVALID_ARGUMENT);

    private final FirebaseMessaging firebaseMessaging;

    public FcmClient(FirebaseProperties firebaseProperties) {
        this.firebaseMessaging = initialize(firebaseProperties);
    }

    public boolean isEnabled() {
        return firebaseMessaging != null;
    }

    public List<String> send(List<String> tokens, String title, String body, Map<String, String> data, boolean dryRun) {
        List<String> invalidTokens = new ArrayList<>();
        if (!isEnabled() || tokens.isEmpty()) {
            return invalidTokens;
        }

        for (int from = 0; from < tokens.size(); from += MAX_TOKENS_PER_REQUEST) {
            List<String> batch = tokens.subList(from, Math.min(from + MAX_TOKENS_PER_REQUEST, tokens.size()));
            invalidTokens.addAll(sendBatch(batch, title, body, data, dryRun));
        }
        return invalidTokens;
    }

    private List<String> sendBatch(List<String> tokens, String title, String body, Map<String, String> data,
                                   boolean dryRun) {
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
                .setApnsConfig(ApnsConfig.builder().setAps(Aps.builder().setSound("default").build()).build())
                .build();

        List<String> invalidTokens = new ArrayList<>();
        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message, dryRun);
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                FirebaseMessagingException exception = responses.get(i).getException();
                if (exception != null && INVALID_TOKEN_ERRORS.contains(exception.getMessagingErrorCode())) {
                    invalidTokens.add(tokens.get(i));
                } else if (exception != null) {
                    log.warn("FCM 푸시 발송 실패 errorCode={}", exception.getMessagingErrorCode(), exception);
                }
            }
        } catch (FirebaseMessagingException e) {
            log.error("FCM 푸시 요청 실패", e);
        }
        return invalidTokens;
    }

    private FirebaseMessaging initialize(FirebaseProperties properties) {
        try (InputStream credentials = openCredentials(properties)) {
            if (credentials == null) {
                log.warn("Firebase 서비스 계정 키가 설정되지 않아 푸시 알림을 보내지 않습니다.");
                return null;
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            return FirebaseMessaging.getInstance(app);
        } catch (IOException | IllegalArgumentException e) {
            log.error("Firebase 초기화에 실패해 푸시 알림을 보내지 않습니다.", e);
            return null;
        }
    }

    private InputStream openCredentials(FirebaseProperties properties) throws IOException {
        if (properties.credentialsBase64() != null && !properties.credentialsBase64().isBlank()) {
            return new ByteArrayInputStream(Base64.getDecoder().decode(properties.credentialsBase64().strip()));
        }
        if (properties.credentialsPath() != null && !properties.credentialsPath().isBlank()) {
            return new FileInputStream(properties.credentialsPath());
        }
        return null;
    }
}
