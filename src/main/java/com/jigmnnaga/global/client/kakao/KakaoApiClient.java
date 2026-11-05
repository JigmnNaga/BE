package com.jigmnnaga.global.client.kakao;

import com.jigmnnaga.application.dto.response.kakao.KakaoUserInfoResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@EnableConfigurationProperties(KakaoProperties.class)
public class KakaoApiClient {

    private static final String KAKAO_USER_INFO_URI = "/v2/user/me";
    private static final String KAKAO_UNLINK_URI = "/v1/user/unlink";
    private static final String KAKAO_ADMIN_KEY_PREFIX = "KakaoAK ";

    private final RestClient restClient;
    private final String adminKey;

    public KakaoApiClient(KakaoProperties kakaoProperties) {
        this.restClient = RestClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .build();
        this.adminKey = kakaoProperties.adminKey();
    }

    public KakaoUserInfoResponse getUserInfo(String kakaoAccessToken) {
        try {
            return restClient.get()
                    .uri(KAKAO_USER_INFO_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.KAKAO_TOKEN_INVALID);
        }
    }

    public void unlink(Long kakaoId) {
        if (adminKey == null || adminKey.isBlank()) {
            log.warn("KAKAO_ADMIN_KEY가 설정되지 않아 카카오 연결 끊기를 건너뜁니다. kakaoId={}", kakaoId);
            return;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target_id_type", "user_id");
        form.add("target_id", String.valueOf(kakaoId));

        try {
            restClient.post()
                    .uri(KAKAO_UNLINK_URI)
                    .header(HttpHeaders.AUTHORIZATION, KAKAO_ADMIN_KEY_PREFIX + adminKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("카카오 연결 끊기에 실패했지만 탈퇴를 계속 진행합니다. kakaoId={}, status={}, body={}",
                    kakaoId, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.warn("카카오 연결 끊기 요청에 실패했지만 탈퇴를 계속 진행합니다. kakaoId={}", kakaoId, e);
        }
    }
}
