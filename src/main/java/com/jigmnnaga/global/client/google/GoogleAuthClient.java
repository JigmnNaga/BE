package com.jigmnnaga.global.client.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableConfigurationProperties(GoogleProperties.class)
public class GoogleAuthClient {

    private final List<String> clientIds;
    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthClient(GoogleProperties googleProperties) {
        this.clientIds = googleProperties.validClientIds();
        // 구글 공개키로 서명을 검증하고, 발급자(accounts.google.com)·대상(우리 클라이언트 ID)·만료를 확인한다.
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(clientIds)
                .build();
    }

    public GoogleUserInfo verify(String idToken) {
        if (clientIds.isEmpty()) {
            throw new BusinessException(ErrorCode.GOOGLE_LOGIN_UNAVAILABLE);
        }

        GoogleIdToken parsedToken = parse(idToken);
        boolean verified;
        try {
            verified = verifier.verify(parsedToken);
        } catch (GeneralSecurityException | IOException e) {
            log.error("구글 공개키를 가져오지 못해 ID 토큰을 검증할 수 없습니다.", e);
            throw new BusinessException(ErrorCode.GOOGLE_LOGIN_FAILED);
        }
        if (!verified) {
            throw new BusinessException(ErrorCode.GOOGLE_TOKEN_INVALID);
        }

        GoogleIdToken.Payload payload = parsedToken.getPayload();
        return new GoogleUserInfo(
                payload.getSubject(),
                (String) payload.get("name"),
                Boolean.TRUE.equals(payload.getEmailVerified()) ? payload.getEmail() : null,
                (String) payload.get("picture"));
    }

    private GoogleIdToken parse(String idToken) {
        try {
            return GoogleIdToken.parse(GsonFactory.getDefaultInstance(), idToken);
        } catch (IOException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.GOOGLE_TOKEN_INVALID);
        }
    }
}
