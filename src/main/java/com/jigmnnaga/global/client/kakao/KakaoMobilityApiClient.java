package com.jigmnnaga.global.client.kakao;

import com.jigmnnaga.application.dto.response.kakao.KakaoCarRouteResponse;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@EnableConfigurationProperties(KakaoProperties.class)
public class KakaoMobilityApiClient {

    private static final String DIRECTIONS_URI = "/v1/directions";
    private static final String KAKAO_REST_API_KEY_PREFIX = "KakaoAK ";
    private static final int SUCCESS_RESULT_CODE = 0;

    private final RestClient restClient;
    private final String restApiKey;

    public KakaoMobilityApiClient(KakaoProperties kakaoProperties) {
        this.restClient = RestClient.builder()
                .baseUrl("https://apis-navi.kakaomobility.com")
                .build();
        this.restApiKey = kakaoProperties.restApiKey();
    }

    public Optional<KakaoCarRouteResponse.Route> getCarRoute(double startLongitude, double startLatitude,
                                                            double endLongitude, double endLatitude) {
        if (restApiKey == null || restApiKey.isBlank()) {
            return Optional.empty();
        }

        try {
            KakaoCarRouteResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(DIRECTIONS_URI)
                            .queryParam("origin", startLongitude + "," + startLatitude)
                            .queryParam("destination", endLongitude + "," + endLatitude)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, KAKAO_REST_API_KEY_PREFIX + restApiKey)
                    .retrieve()
                    .body(KakaoCarRouteResponse.class);

            if (response == null || response.routes() == null || response.routes().isEmpty()) {
                return Optional.empty();
            }
            KakaoCarRouteResponse.Route route = response.routes().getFirst();
            if (route.resultCode() == null || route.resultCode() != SUCCESS_RESULT_CODE || route.summary() == null) {
                log.warn("카카오모빌리티 길찾기 결과 없음 resultCode={}", route.resultCode());
                return Optional.empty();
            }
            return Optional.of(route);
        } catch (RestClientException e) {
            log.warn("카카오모빌리티 길찾기 요청 실패", e);
            return Optional.empty();
        }
    }
}
