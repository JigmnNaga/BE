
package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.TransitRouteRequest;
import com.jigmnnaga.application.dto.response.TransitRouteResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoTransitRouteResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.TransitRouteMapper;
import com.jigmnnaga.global.cache.TransitRouteCache;
import com.jigmnnaga.global.client.kakao.KakaoTransitApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransitRouteGetUseCase {

    private final KakaoTransitApiClient kakaoTransitApiClient;
    private final TransitRouteMapper transitRouteMapper;
    private final TransitRouteCache transitRouteCache;

    public TransitRouteResponse getRoutes(TransitRouteRequest request) {

        // 1. Redis 캐시 조회
        var cached = transitRouteCache.get(request);

        if (cached.isPresent()) {
            return cached.get();
        }

        // 2. 캐시가 없으면 카카오 API 호출
        KakaoTransitRouteResponse response = kakaoTransitApiClient.getRoutes(
                request.startLongitude(),
                request.startLatitude(),
                request.endLongitude(),
                request.endLatitude()
        );

        if (response == null || !"OK".equals(response.status()) || response.routes() == null) {
            throw new BusinessException(ErrorCode.TRANSIT_ROUTE_FAILED);
        }

        // 3. 응답 변환
        TransitRouteResponse result = transitRouteMapper.toTransitRouteResponse(response);

        // 4. Redis 캐시 저장
        transitRouteCache.put(request, result);

        return result;
    }
}