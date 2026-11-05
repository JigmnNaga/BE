package com.jigmnnaga.global.client.kakao;

import com.jigmnnaga.application.dto.response.kakao.KakaoTransitRouteResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@EnableConfigurationProperties(KakaoProperties.class)
public class KakaoTransitApiClient {

    private static final String TRANSIT_ROUTE_URI = "/v2/routing/publictraffic";
    private static final String KAKAO_REST_API_KEY_PREFIX = "KakaoAK ";

    private final RestClient restClient;
    private final String restApiKey;

    public KakaoTransitApiClient(KakaoProperties kakaoProperties) {
        this.restClient = RestClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .build();
        this.restApiKey = kakaoProperties.restApiKey();
    }

    public KakaoTransitRouteResponse getRoutes(
            double startLongitude,
            double startLatitude,
            double endLongitude,
            double endLatitude
    ) {
        if (restApiKey == null || restApiKey.isBlank()) {
            throw new BusinessException(ErrorCode.TRANSIT_ROUTE_UNAVAILABLE);
        }

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(TRANSIT_ROUTE_URI)
                            .queryParam("start_x", startLongitude)
                            .queryParam("start_y", startLatitude)
                            .queryParam("end_x", endLongitude)
                            .queryParam("end_y", endLatitude)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, KAKAO_REST_API_KEY_PREFIX + restApiKey)
                    .retrieve()
                    .body(KakaoTransitRouteResponse.class);

        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("카카오 대중교통 API 호출 한도 초과");
                throw new BusinessException(ErrorCode.TRANSIT_ROUTE_QUOTA_EXCEEDED);
            }

            log.warn("카카오 대중교통 경로 조회 실패 status={}", e.getStatusCode());
            throw new BusinessException(ErrorCode.TRANSIT_ROUTE_FAILED);

        } catch (RestClientException e) {
            log.warn("카카오 대중교통 경로 조회 요청 실패", e);
            throw new BusinessException(ErrorCode.TRANSIT_ROUTE_FAILED);
        }
    }
}