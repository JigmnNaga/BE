package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.response.PlaceSearchResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoPlaceSearchResponse;
import com.jigmnnaga.application.dto.response.kakao.KakaoPlaceSearchResponse.KakaoPlace;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlaceMapper {

    private static final String CATEGORY_SEPARATOR = " > ";
    private static final String LINE_SEPARATOR = ", ";
    private static final String METROPOLITAN_PREFIX = "수도권";
    private static final double SAME_STATION_DISTANCE_METERS = 1_000;
    private static final double EARTH_RADIUS_METERS = 6_371_000;

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
        List<List<KakaoPlace>> stationGroups = new ArrayList<>();
        for (KakaoPlace place : response.documents()) {
            stationGroups.stream()
                    .filter(group -> isSameStation(group.getFirst(), place))
                    .findFirst()
                    .ifPresentOrElse(group -> group.add(place), () -> stationGroups.add(new ArrayList<>(List.of(place))));
        }

        List<PlaceSearchResponse.PlaceResponse> stations = stationGroups.stream()
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

    private boolean isSameStation(KakaoPlace station, KakaoPlace place) {
        return stationName(station).equals(stationName(place))
                && distanceMeters(station, place) <= SAME_STATION_DISTANCE_METERS;
    }

    private double distanceMeters(KakaoPlace from, KakaoPlace to) {
        double fromLatitude = Math.toRadians(Double.parseDouble(from.y()));
        double toLatitude = Math.toRadians(Double.parseDouble(to.y()));
        double latitudeDelta = toLatitude - fromLatitude;
        double longitudeDelta = Math.toRadians(Double.parseDouble(to.x()) - Double.parseDouble(from.x()));

        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(fromLatitude) * Math.cos(toLatitude) * Math.pow(Math.sin(longitudeDelta / 2), 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(haversine));
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

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
