package com.jigmnnaga.global.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.global.client.google.GoogleAuthClient;
import com.jigmnnaga.global.client.google.GoogleProperties;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GoogleAuthClientTest {

    private ErrorCode errorCodeOf(GoogleAuthClient client, String idToken) {
        return catchThrowableOfType(BusinessException.class, () -> client.verify(idToken)).getErrorCode();
    }

    @Test
    @DisplayName("클라이언트 ID가 설정되지 않으면 구글 로그인을 사용할 수 없다")
    void unavailableWithoutClientId() {
        GoogleAuthClient client = new GoogleAuthClient(new GoogleProperties(List.of(" ")));

        assertThat(errorCodeOf(client, "any-token")).isEqualTo(ErrorCode.GOOGLE_LOGIN_UNAVAILABLE);
    }

    @Test
    @DisplayName("형식이 잘못된 토큰은 유효하지 않은 토큰으로 처리한다")
    void malformedToken() {
        GoogleAuthClient client = new GoogleAuthClient(new GoogleProperties(List.of("web-client-id")));

        assertThat(errorCodeOf(client, "not-a-jwt")).isEqualTo(ErrorCode.GOOGLE_TOKEN_INVALID);
    }

    @Test
    @DisplayName("다른 앱용으로 발급된(aud가 다른) 토큰은 서명 확인 전에 거부한다")
    void wrongAudience() {
        GoogleAuthClient client = new GoogleAuthClient(new GoogleProperties(List.of("web-client-id")));
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"RS256\",\"kid\":\"test\"}".getBytes());
        String payload = encoder.encodeToString(("{\"iss\":\"https://accounts.google.com\",\"aud\":\"other-client-id\","
                + "\"sub\":\"123\",\"iat\":" + (System.currentTimeMillis() / 1000)
                + ",\"exp\":" + (System.currentTimeMillis() / 1000 + 3600) + "}").getBytes());
        String token = header + "." + payload + "." + encoder.encodeToString("signature".getBytes());

        assertThat(errorCodeOf(client, token)).isEqualTo(ErrorCode.GOOGLE_TOKEN_INVALID);
    }
}
