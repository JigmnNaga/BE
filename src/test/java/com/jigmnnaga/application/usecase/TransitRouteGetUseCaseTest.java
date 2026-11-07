
package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.TransitRouteRequest;
import com.jigmnnaga.application.dto.response.TransitRouteResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoTransitRouteResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.mapper.TransitRouteMapper;
import com.jigmnnaga.global.cache.TransitRouteCache;
import com.jigmnnaga.global.client.kakao.KakaoTransitApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransitRouteGetUseCaseTest {

    @Mock
    private KakaoTransitApiClient kakaoTransitApiClient;

    @Mock
    private TransitRouteMapper transitRouteMapper;

    @Mock
    private TransitRouteCache transitRouteCache;

    @InjectMocks
    private TransitRouteGetUseCase useCase;

    private TransitRouteRequest request;
    private TransitRouteResponse result;

    @BeforeEach
    void setUp() {
        request = new TransitRouteRequest(
                127.0, 37.5, 127.1, 37.6
        );
        result = new TransitRouteResponse(0, List.of());
    }

    @Test
    void 캐시가_있으면_카카오_API를_호출하지_않는다() {
        when(transitRouteCache.get(request))
                .thenReturn(Optional.of(result));

        TransitRouteResponse response = useCase.getRoutes(request);

        assertThat(response).isSameAs(result);
        verifyNoInteractions(kakaoTransitApiClient, transitRouteMapper);
        verify(transitRouteCache, never()).put(any(), any());
    }

    @Test
    void 캐시가_없으면_카카오_API를_호출하고_결과를_저장한다() {
        KakaoTransitRouteResponse kakaoResponse =
                mock(KakaoTransitRouteResponse.class);

        when(transitRouteCache.get(request))
                .thenReturn(Optional.empty());
        when(kakaoTransitApiClient.getRoutes(
                127.0, 37.5, 127.1, 37.6
        )).thenReturn(kakaoResponse);
        when(kakaoResponse.status()).thenReturn("OK");
        when(kakaoResponse.routes()).thenReturn(List.of());
        when(transitRouteMapper.toTransitRouteResponse(kakaoResponse))
                .thenReturn(result);

        TransitRouteResponse response = useCase.getRoutes(request);

        assertThat(response).isSameAs(result);
        verify(transitRouteCache).put(request, result);
        verify(kakaoTransitApiClient, times(1))
                .getRoutes(127.0, 37.5, 127.1, 37.6);
    }

    @Test
    void 카카오_API_응답이_비정상이면_예외가_발생한다() {
        when(transitRouteCache.get(request))
                .thenReturn(Optional.empty());
        when(kakaoTransitApiClient.getRoutes(
                127.0, 37.5, 127.1, 37.6
        )).thenReturn(null);

        assertThatThrownBy(() -> useCase.getRoutes(request))
                .isInstanceOf(BusinessException.class);

        verify(transitRouteCache, never()).put(any(), any());
    }
}