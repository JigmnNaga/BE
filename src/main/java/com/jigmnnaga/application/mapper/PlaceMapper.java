package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.response.PlaceSearchResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoPlaceSearchResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoPlaceSearchResponse.KakaoPlace;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PlaceMapper {

    private static final String CATEGORY_SEPARATOR = " > ";
    private static final String LINE_SEPARATOR = ", ";
    private static final String METROPOLITAN_PREFIX = "수도권";

    public PlaceSearchResponse toPlaceSearchResponse(KakaoPlaceSearchResponse response) {
        List<PlaceSearchResponse.PlaceResponse> places = response.documents().stream()
                .map(place -> toPlaceResponse(place, place.placeName(), null))
                .toList();

        return PlaceSearchResponse.builder()
                .places(places)
                .isEnd(response.meta().isEnd())
                .build();
    }

    public PlaceSearchResponse toStationSearchResponse(KakaoPlaceSearchResponse response) {
        Map<String, List<KakaoPlace>> platformsByStation = new LinkedHashMap<>();
        for (KakaoPlace place : response.documents()) {
            platformsByStation.computeIfAbsent(stationKey(place), key -> new ArrayList<>()).add(place);
        }

        List<PlaceSearchResponse.PlaceResponse> stations = platformsByStation.values().stream()
                .map(platforms -> {
                    KakaoPlace representative = platforms.getFirst();
                    String lineNames = String.join(LINE_SEPARATOR, platforms.stream()
                            .map(this::lineName)
                            .distinct()
                            .toList());
                    return toPlaceResponse(representative, stationName(representative), lineNames);
                })
                .toList();

        return PlaceSearchResponse.builder()
                .places(stations)
                .isEnd(response.meta().isEnd())
                .build();
    }

    private PlaceSearchResponse.PlaceResponse toPlaceResponse(KakaoPlace place, String placeName, String lineNames) {
        return PlaceSearchResponse.PlaceResponse.builder()
                .kakaoPlaceId(place.id())
                .placeName(placeName)
                .lineNames(lineNames)
                .roadAddress(emptyToNull(place.roadAddressName()))
                .jibunAddress(emptyToNull(place.addressName()))
                .latitude(Double.valueOf(place.y()))
                .longitude(Double.valueOf(place.x()))
                .build();
    }

    private String stationKey(KakaoPlace place) {
        return stationName(place) + "|" + regionOf(place.addressName());
    }

    private String stationName(KakaoPlace place) {
        String placeName = place.placeName().strip();
        int spaceIndex = placeName.indexOf(' ');
        return spaceIndex < 0 ? placeName : placeName.substring(0, spaceIndex);
    }

    private String lineName(KakaoPlace place) {
        String placeName = place.placeName().strip();
        int spaceIndex = placeName.indexOf(' ');
        if (spaceIndex >= 0) {
            return placeName.substring(spaceIndex + 1).strip();
        }
        String categoryName = place.categoryName();
        if (categoryName == null || categoryName.isBlank()) {
            return placeName;
        }
        String categoryLineName = categoryName.substring(
                categoryName.lastIndexOf(CATEGORY_SEPARATOR) + CATEGORY_SEPARATOR.length());
        return categoryLineName.startsWith(METROPOLITAN_PREFIX)
                ? categoryLineName.substring(METROPOLITAN_PREFIX.length())
                : categoryLineName;
    }

    private String regionOf(String addressName) {
        if (addressName == null) {
            return "";
        }
        String[] tokens = addressName.split(" ");
        return String.join(" ", List.of(tokens).subList(0, Math.min(2, tokens.length)));
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
