
package com.jigmnnaga.global.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InviteCodeCache {

    private static final String KEY_PREFIX = "invite:code:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, String> redisTemplate;

    public Optional<Long> get(String inviteCode) {
        try {
            String value = redisTemplate.opsForValue()
                    .get(KEY_PREFIX + inviteCode);

            return value == null
                    ? Optional.empty()
                    : Optional.of(Long.valueOf(value));
        } catch (Exception e) {
            log.warn("초대 코드 캐시 조회 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void put(String inviteCode, Long appointmentId) {
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + inviteCode,
                    appointmentId.toString(),
                    CACHE_TTL
            );
        } catch (Exception e) {
            log.warn("초대 코드 캐시 저장 실패: {}", e.getMessage());
        }
    }

    public void evict(String inviteCode) {
        try {
            redisTemplate.delete(KEY_PREFIX + inviteCode);
        } catch (Exception e) {
            log.warn("초대 코드 캐시 삭제 실패: {}", e.getMessage());
        }
    }
}