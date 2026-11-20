package com.jigmnnaga.global.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class EtaCacheTest {

    @Autowired
    private EtaCache etaCache;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final long baseAppointmentId = 99900L + System.currentTimeMillis() % 10000;
    private final long baseMemberId = 99901L;

    @AfterEach
    void cleanUp() {
        redisTemplate.delete("eta:result:" + baseAppointmentId);
        redisTemplate.delete(etaCache.throttleKey(baseAppointmentId, baseMemberId));
    }

    @Test
    @DisplayName("tryAcquireCalculation_최초_호출은_true를_반환한다")
    void tryAcquireCalculation_최초_호출은_true를_반환한다() {
        boolean result = etaCache.tryAcquireCalculation(baseAppointmentId, baseMemberId);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("tryAcquireCalculation_중복_호출은_false를_반환한다")
    void tryAcquireCalculation_중복_호출은_false를_반환한다() {
        etaCache.tryAcquireCalculation(baseAppointmentId, baseMemberId);
        boolean second = etaCache.tryAcquireCalculation(baseAppointmentId, baseMemberId);
        assertThat(second).isFalse();
    }

    @Test
    @DisplayName("putIfNewer_최신_데이터를_저장한다")
    void putIfNewer_최신_데이터를_저장한다() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eta = now.plusMinutes(10);

        boolean result = etaCache.putIfNewer(baseAppointmentId, baseMemberId, eta, 10, now);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("putIfNewer_기존과_동일한_calculatedAt이면_거부한다")
    void putIfNewer_기존과_동일한_calculatedAt이면_거부한다() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eta = now.plusMinutes(10);

        etaCache.putIfNewer(baseAppointmentId, baseMemberId, eta, 10, now);
        boolean result = etaCache.putIfNewer(baseAppointmentId, baseMemberId, eta, 5, now);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("putIfNewer_오래된_calculatedAt이면_거부한다")
    void putIfNewer_오래된_calculatedAt이면_거부한다() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime older = now.minusSeconds(10);
        LocalDateTime eta = now.plusMinutes(10);

        etaCache.putIfNewer(baseAppointmentId, baseMemberId, eta, 10, now);
        boolean result = etaCache.putIfNewer(baseAppointmentId, baseMemberId, eta, 3, older);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getLastCalculatedAt_저장된_계산_시각을_반환한다")
    void getLastCalculatedAt_저장된_계산_시각을_반환한다() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eta = now.plusMinutes(10);
        etaCache.putIfNewer(baseAppointmentId, baseMemberId, eta, 10, now);

        java.util.Optional<LocalDateTime> result = etaCache.getLastCalculatedAt(baseAppointmentId, baseMemberId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(now);
    }

    @Test
    @DisplayName("getLastCalculatedAt_존재하지_않으면_empty를_반환한다")
    void getLastCalculatedAt_존재하지_않으면_empty를_반환한다() {
        java.util.Optional<LocalDateTime> result = etaCache.getLastCalculatedAt(99999L, 99999L);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("evictResultsByAppointment_저장된_ETA_캐시를_삭제한다")
    void evictResultsByAppointment_저장된_ETA_캐시를_삭제한다() {
        LocalDateTime now = LocalDateTime.now();
        etaCache.putIfNewer(baseAppointmentId, baseMemberId, now.plusMinutes(10), 10, now);

        etaCache.evictResultsByAppointment(baseAppointmentId);

        assertThat(etaCache.getLastCalculatedAt(baseAppointmentId, baseMemberId)).isEmpty();
    }

    @Test
    @DisplayName("evictResultsByAppointment_후_putIfNewer는_항상_최신으로_저장된다")
    void evictResultsByAppointment_후_putIfNewer는_항상_최신으로_저장된다() {
        LocalDateTime now = LocalDateTime.now();
        etaCache.putIfNewer(baseAppointmentId, baseMemberId, now.plusMinutes(10), 10, now);
        etaCache.evictResultsByAppointment(baseAppointmentId);

        // 무효화 이후에는 동일 calculatedAt이라도 저장 성공해야 한다
        boolean result = etaCache.putIfNewer(baseAppointmentId, baseMemberId, now.plusMinutes(5), 5, now);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("putIfNewer_TTL이_설정된다")
    void putIfNewer_TTL이_설정된다() {
        LocalDateTime now = LocalDateTime.now();
        etaCache.putIfNewer(baseAppointmentId, baseMemberId, now.plusMinutes(10), 10, now);

        Long ttl = redisTemplate.getExpire("eta:result:" + baseAppointmentId);

        assertThat(ttl).isNotNull();
        assertThat(ttl).isPositive();
        assertThat(ttl).isLessThanOrEqualTo(600L); // 10분 = 600초
    }
}
