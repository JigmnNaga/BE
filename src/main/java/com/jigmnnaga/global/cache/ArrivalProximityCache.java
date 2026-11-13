package com.jigmnnaga.global.cache;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * GPS 도착 감지를 위한 Redis 캐시.
 *
 * <ul>
 *   <li>proximity key: 연속 반경 내 관측 횟수 (TTL 10분)</li>
 *   <li>claimed key:   도착 중복 처리 방지용 SETNX 잠금 (TTL 5초)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArrivalProximityCache {

    private static final Duration PROXIMITY_TTL = Duration.ofMinutes(10);
    private static final Duration CLAIM_TTL = Duration.ofSeconds(5);

    private static final String PROXIMITY_KEY_PREFIX = "arrival:proximity:";
    private static final String CLAIMED_KEY_PREFIX = "arrival:claimed:";

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 반경 내 관측 횟수를 증가시키고 증가 후 값을 반환합니다.
     * TTL은 매 호출마다 갱신됩니다.
     */
    public long incrementCount(Long appointmentId, Long memberId) {
        String key = proximityKey(appointmentId, memberId);
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, PROXIMITY_TTL);
        return count == null ? 1L : count;
    }

    /**
     * 반경 내 관측 횟수를 0으로 초기화합니다.
     */
    public void resetCount(Long appointmentId, Long memberId) {
        redisTemplate.delete(proximityKey(appointmentId, memberId));
    }

    /**
     * 도착 처리를 원자적으로 클레임합니다 (중복 처리 방지).
     *
     * @return true: 클레임 성공 (DB 처리 진행), false: 이미 처리 중
     */
    public boolean tryClaimArrival(Long appointmentId, Long memberId) {
        String key = claimedKey(appointmentId, memberId);
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", CLAIM_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 도착 클레임을 해제합니다 (트랜잭션 롤백 시 복구용).
     */
    public void releaseClaim(Long appointmentId, Long memberId) {
        redisTemplate.delete(claimedKey(appointmentId, memberId));
    }

    public String proximityKey(Long appointmentId, Long memberId) {
        return PROXIMITY_KEY_PREFIX + appointmentId + ":" + memberId;
    }

    public String claimedKey(Long appointmentId, Long memberId) {
        return CLAIMED_KEY_PREFIX + appointmentId + ":" + memberId;
    }
}
