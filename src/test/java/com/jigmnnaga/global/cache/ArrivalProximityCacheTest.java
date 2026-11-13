package com.jigmnnaga.global.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class ArrivalProximityCacheTest {

    @Autowired
    private ArrivalProximityCache arrivalProximityCache;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final long appointmentId = 77700L + System.currentTimeMillis() % 10000;
    private final long memberId = 77701L;

    @AfterEach
    void cleanUp() {
        redisTemplate.delete(arrivalProximityCache.proximityKey(appointmentId, memberId));
        redisTemplate.delete(arrivalProximityCache.claimedKey(appointmentId, memberId));
    }

    @Test
    @DisplayName("incrementCount_최초_호출_시_1을_반환한다")
    void incrementCount_최초_호출_시_1을_반환한다() {
        long count = arrivalProximityCache.incrementCount(appointmentId, memberId);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("incrementCount_연속_호출_시_누적된다")
    void incrementCount_연속_호출_시_누적된다() {
        arrivalProximityCache.incrementCount(appointmentId, memberId);
        long count = arrivalProximityCache.incrementCount(appointmentId, memberId);
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("resetCount_호출_후_incrementCount는_1부터_시작한다")
    void resetCount_호출_후_incrementCount는_1부터_시작한다() {
        arrivalProximityCache.incrementCount(appointmentId, memberId);
        arrivalProximityCache.incrementCount(appointmentId, memberId);

        arrivalProximityCache.resetCount(appointmentId, memberId);

        long count = arrivalProximityCache.incrementCount(appointmentId, memberId);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("tryClaimArrival_최초_호출은_true를_반환한다")
    void tryClaimArrival_최초_호출은_true를_반환한다() {
        boolean result = arrivalProximityCache.tryClaimArrival(appointmentId, memberId);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("tryClaimArrival_중복_호출은_false를_반환한다")
    void tryClaimArrival_중복_호출은_false를_반환한다() {
        arrivalProximityCache.tryClaimArrival(appointmentId, memberId);
        boolean second = arrivalProximityCache.tryClaimArrival(appointmentId, memberId);
        assertThat(second).isFalse();
    }

    @Test
    @DisplayName("releaseClaim_후_tryClaimArrival은_다시_true를_반환한다")
    void releaseClaim_후_tryClaimArrival은_다시_true를_반환한다() {
        arrivalProximityCache.tryClaimArrival(appointmentId, memberId);
        arrivalProximityCache.releaseClaim(appointmentId, memberId);

        boolean result = arrivalProximityCache.tryClaimArrival(appointmentId, memberId);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("incrementCount_TTL이_설정된다")
    void incrementCount_TTL이_설정된다() {
        arrivalProximityCache.incrementCount(appointmentId, memberId);
        Long ttl = redisTemplate.getExpire(arrivalProximityCache.proximityKey(appointmentId, memberId));
        assertThat(ttl).isNotNull().isPositive();
        assertThat(ttl).isLessThanOrEqualTo(600L); // 10분 = 600초
    }
}
