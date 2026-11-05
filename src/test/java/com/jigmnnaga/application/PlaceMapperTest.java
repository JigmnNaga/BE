package com.jigmnnaga.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.jigmnnaga.application.dto.response.PlaceSearchResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoPlaceSearchResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoPlaceSearchResponse.KakaoMeta;
import com.jigmnnaga.application.dto.response.kakao.KakaoPlaceSearchResponse.KakaoPlace;
import com.jigmnnaga.application.mapper.PlaceMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlaceMapperTest {

    private final PlaceMapper placeMapper = new PlaceMapper();

    private KakaoPlace place(String name, String category, String address, String latitude, String longitude) {
        return new KakaoPlace("id-" + name, name, category, address, "", longitude, latitude);
    }

    @Test
    @DisplayName("같은 이름이면서 1km 이내인 역은 노선을 묶고, 멀리 떨어진 동명이역은 분리한다")
    void groupStations() {
        KakaoPlaceSearchResponse response = new KakaoPlaceSearchResponse(List.of(
                place("서울역 1호선", "교통,수송 > 지하철,전철 > 수도권1호선", "서울 중구 봉래동2가", "37.5555", "126.9726"),
                place("서울역 4호선", "교통,수송 > 지하철,전철 > 수도권4호선", "서울 용산구 동자동", "37.5533", "126.9697"),
                place("양평역 5호선", "교통,수송 > 지하철,전철 > 수도권5호선", "서울 영등포구 양평동", "37.5262", "126.8857"),
                place("양평역 경의중앙선", "교통,수송 > 지하철,전철 > 경의중앙선", "경기 양평군 양평읍", "37.4927", "127.4922"),
                place("신논현역", "교통,수송 > 지하철,전철 > 수도권9호선", "서울 강남구 논현동", "37.5048", "127.0255")
        ), new KakaoMeta(true));

        List<PlaceSearchResponse.PlaceResponse> stations = placeMapper.toStationSearchResponse(response).places();

        assertThat(stations).extracting(PlaceSearchResponse.PlaceResponse::placeName)
                .containsExactly("서울역", "양평역", "양평역", "신논현역");
        assertThat(stations.get(0).lineNames()).isEqualTo("1호선, 4호선");
        assertThat(stations.get(3).lineNames()).isEqualTo("9호선");
    }
}
