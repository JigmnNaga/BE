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
class LatenessCacheTest {

    @Autowired
    private LatenessCache latenessCache;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 타임스탬프 기반 고유 key 사용
    private final long baseAppointmentId = 88800L + System.currentTimeMillis() % 10000;
    private final long baseMemberId = 88801L;

    @AfterEach
    void cleanUp() {
        redisTemplate.delete("lateness:" + baseAppointmentId);
    }

    @Test
    @DisplayName("putIfNewer_최신_데이터를_저장한다")
    void putIfNewer_최신_데이터를_저장한다() {
        LocalDateTime now = LocalDateTime.now();

        boolean result = latenessCache.putIfNewer(baseAppointmentId, baseMemberId, 5, true, now);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("putIfNewer_기존과_동일한_calculatedAt이면_거부한다")
    void putIfNewer_기존과_동일한_calculatedAt이면_거부한다() {
        LocalDateTime now = LocalDateTime.now();

        latenessCache.putIfNewer(baseAppointmentId, baseMemberId, 5, true, now);
        boolean result = latenessCache.putIfNewer(baseAppointmentId, baseMemberId, 10, true, now);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("putIfNewer_오래된_calculatedAt이면_거부한다")
    void putIfNewer_오래된_calculatedAt이면_거부한다() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime older = now.minusSeconds(10);

        latenessCache.putIfNewer(baseAppointmentId, baseMemberId, 5, true, now);
        boolean result = latenessCache.putIfNewer(baseAppointmentId, baseMemberId, 3, false, older);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("get_저장된_지각_정보를_반환한다")
    void get_저장된_지각_정보를_반환한다() {
        LocalDateTime now = LocalDateTime.now();
        latenessCache.putIfNewer(baseAppointmentId, baseMemberId, 7, true, now);

        java.util.Optional<LatenessCache.LatenessInfo> result = latenessCache.get(baseAppointmentId, baseMemberId);

        assertThat(result).isPresent();
        LatenessCache.LatenessInfo info = result.get();
        assertThat(info.expectedLatenessMinutes()).isEqualTo(7);
        assertThat(info.isExpectedLate()).isTrue();
        assertThat(info.calculatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("get_존재하지_않으면_empty를_반환한다")
    void get_존재하지_않으면_empty를_반환한다() {
        java.util.Optional<LatenessCache.LatenessInfo> result = latenessCache.get(99999L, 99999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("putIfNewer_TTL이_설정된다")
    void putIfNewer_TTL이_설정된다() {
        LocalDateTime now = LocalDateTime.now();
        latenessCache.putIfNewer(baseAppointmentId, baseMemberId, 0, false, now);

        Long ttl = redisTemplate.getExpire("lateness:" + baseAppointmentId);

        assertThat(ttl).isNotNull();
        assertThat(ttl).isPositive();
        assertThat(ttl).isLessThanOrEqualTo(600L); // 10분 = 600초
    }
}
