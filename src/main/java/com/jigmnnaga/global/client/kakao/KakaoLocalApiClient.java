package com.jigmnnaga.global.client.kakao;

import com.jigmnnaga.application.dto.response.kakao.KakaoPlaceSearchResponse;
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
public class KakaoLocalApiClient {

    private static final String KEYWORD_SEARCH_URI = "/v2/local/search/keyword.json";
    private static final String KAKAO_REST_API_KEY_PREFIX = "KakaoAK ";

    private final RestClient restClient;
    private final String restApiKey;

    public KakaoLocalApiClient(KakaoProperties kakaoProperties) {
        this.restClient = RestClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .build();
        this.restApiKey = kakaoProperties.restApiKey();
    }

    public KakaoPlaceSearchResponse searchByKeyword(String query, String categoryGroupCode, int page, int size) {
        if (restApiKey == null || restApiKey.isBlank()) {
            throw new BusinessException(ErrorCode.PLACE_SEARCH_UNAVAILABLE);
        }

        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(KEYWORD_SEARCH_URI)
                                .queryParam("query", query)
                                .queryParam("page", page)
                                .queryParam("size", size);
                        if (categoryGroupCode != null) {
                            uriBuilder.queryParam("category_group_code", categoryGroupCode);
                        }
                        return uriBuilder.build();
                    })
                    .header(HttpHeaders.AUTHORIZATION, KAKAO_REST_API_KEY_PREFIX + restApiKey)
                    .retrieve()
                    .body(KakaoPlaceSearchResponse.class);
        } catch (RestClientResponseException e) {
            log.warn("카카오 장소 검색 실패 status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.PLACE_SEARCH_FAILED);
        } catch (RestClientException e) {
            log.warn("카카오 장소 검색 요청 실패", e);
            throw new BusinessException(ErrorCode.PLACE_SEARCH_FAILED);
        }
    }
}
