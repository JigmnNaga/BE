package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.TransitRouteRequest;
import com.jigmnnaga.application.dto.response.TransitRouteResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoTransitRouteResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.TransitRouteMapper;
import com.jigmnnaga.global.client.kakao.KakaoTransitApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransitRouteGetUseCase {

    private final KakaoTransitApiClient kakaoTransitApiClient;
    private final TransitRouteMapper transitRouteMapper;

    public TransitRouteResponse getRoutes(TransitRouteRequest request) {
        KakaoTransitRouteResponse response = kakaoTransitApiClient.getRoutes(
                request.startLongitude(),
                request.startLatitude(),
                request.endLongitude(),
                request.endLatitude()
        );

        if (response == null || !"OK".equals(response.status()) || response.routes() == null) {
            throw new BusinessException(ErrorCode.TRANSIT_ROUTE_FAILED);
        }

        return transitRouteMapper.toTransitRouteResponse(response);
    }
}