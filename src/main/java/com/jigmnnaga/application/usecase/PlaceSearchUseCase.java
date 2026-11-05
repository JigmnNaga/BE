package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.PlaceSearchType;
import com.jigmnnaga.application.dto.response.PlaceSearchResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoPlaceSearchResponse;
import com.jigmnnaga.application.mapper.PlaceMapper;
import com.jigmnnaga.global.client.kakao.KakaoLocalApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlaceSearchUseCase {

    private static final String SUBWAY_CATEGORY_GROUP_CODE = "SW8";

    private final KakaoLocalApiClient kakaoLocalApiClient;
    private final PlaceMapper placeMapper;

    public PlaceSearchResponse searchPlaces(String query, PlaceSearchType type, int page, int size) {
        if (type == PlaceSearchType.STATION) {
            KakaoPlaceSearchResponse response = kakaoLocalApiClient.searchByKeyword(
                    query.strip(), SUBWAY_CATEGORY_GROUP_CODE, page, size);
            return placeMapper.toStationSearchResponse(response);
        }

        KakaoPlaceSearchResponse response = kakaoLocalApiClient.searchByKeyword(query.strip(), null, page, size);
        return placeMapper.toPlaceSearchResponse(response);
    }
}
