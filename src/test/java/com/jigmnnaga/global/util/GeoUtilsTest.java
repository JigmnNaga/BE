package com.jigmnnaga.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeoUtilsTest {

    @Test
    @DisplayName("동일_좌표_거리는_0m")
    void 동일_좌표_거리는_0m() {
        double dist = GeoUtils.distanceMeters(37.4979, 127.0276, 37.4979, 127.0276);
        assertThat(dist).isLessThan(0.001);
    }

    @Test
    @DisplayName("약_111m_떨어진_두_좌표의_거리_계산")
    void 약_111m_떨어진_두_좌표의_거리_계산() {
        // 위도 0.001도 차이 ≈ 약 111m
        double dist = GeoUtils.distanceMeters(37.4979, 127.0276, 37.4989, 127.0276);
        assertThat(dist).isBetween(100.0, 120.0);
    }

    @Test
    @DisplayName("100m_미만이면_반경_내로_판단할_수_있다")
    void 거리가_100m_미만이면_반경_내로_판단할_수_있다() {
        // 약 50m 거리
        double dist = GeoUtils.distanceMeters(37.4979, 127.0276, 37.4983, 127.0276);
        assertThat(dist).isLessThan(100.0);
    }

    @Test
    @DisplayName("100m_초과이면_반경_외로_판단할_수_있다")
    void 거리가_100m_초과이면_반경_외로_판단할_수_있다() {
        // 약 500m 거리
        double dist = GeoUtils.distanceMeters(37.4979, 127.0276, 37.5024, 127.0276);
        assertThat(dist).isGreaterThan(100.0);
    }
}
