package com.jigmnnaga.global.cache;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class EtaCache {

    /**
     * ETA 재계산 최소 간격 (30초)
     * 쓰로틀 키의 TTL로 사용됩니다.
     */
    static final Duration THROTTLE_TTL = Duration.ofSeconds(30);
    private static final Duration RESULT_TTL = Duration.ofMinutes(10);

    /**
     * SETNX 키: "eta:throttle:{appointmentId}:{memberId}" → "1" (TTL 30s)
     * Redis 원자적 연산으로 경쟁 조건 없이 한 번만 계산이 실행되도록 보장합니다.
     */
    private static final String THROTTLE_KEY_PREFIX = "eta:throttle:";

    /**
     * Hash 키: "eta:result:{appointmentId}" → field: "{memberId}" → JSON
     * ETA 계산 결과를 저장합니다.
     */
    private static final String RESULT_KEY_PREFIX = "eta:result:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * ETA 계산 권한을 원자적으로 획득합니다.
     * 같은 appointmentId + memberId에 대해 THROTTLE_TTL 내에 한 번만 true를 반환합니다.
     *
     * @return true: 계산 진행, false: 쓰로틀 중이므로 건너뜀
     */
    public boolean tryAcquireCalculation(Long appointmentId, Long memberId) {
        String key = THROTTLE_KEY_PREFIX + appointmentId + ":" + memberId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", THROTTLE_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * ETA 결과를 저장합니다. calculatedAt이 기존 항목보다 최신일 때만 덮어씁니다.
     * 오래된 계산 결과가 최신 결과를 덮어쓰는 것을 방지합니다.
     *
     * @return true: 저장됨 (브로드캐스트 진행), false: 오래된 결과이므로 건너뜀
     */
    public boolean putIfNewer(Long appointmentId, Long memberId,
                               LocalDateTime estimatedArrivalTime, int remainingMinutes,
                               LocalDateTime calculatedAt) {
        String key = RESULT_KEY_PREFIX + appointmentId;
        String field = String.valueOf(memberId);
        try {
            Object rawValue = redisTemplate.opsForHash().get(key, field);
            if (rawValue != null) {
                Map<?, ?> existing = objectMapper.readValue(rawValue.toString(), Map.class);
                String existingStr = (String) existing.get("calculatedAt");
                if (existingStr != null && !LocalDateTime.parse(existingStr).isBefore(calculatedAt)) {
                    return false; // 기존 항목이 같거나 더 최신
                }
            }
            Map<String, String> entry = Map.of(
                    "estimatedArrivalTime", estimatedArrivalTime.toString(),
                    "remainingMinutes", String.valueOf(remainingMinutes),
                    "calculatedAt", calculatedAt.toString()
            );
            redisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(entry));
            redisTemplate.expire(key, RESULT_TTL);
            return true;
        } catch (Exception e) {
            log.warn("ETA 캐시 저장 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 마지막 ETA 계산 시각을 반환합니다.
     * 테스트 및 ETA 결과 조회에 사용됩니다.
     */
    public Optional<LocalDateTime> getLastCalculatedAt(Long appointmentId, Long memberId) {
        String key = RESULT_KEY_PREFIX + appointmentId;
        String field = String.valueOf(memberId);
        try {
            Object rawValue = redisTemplate.opsForHash().get(key, field);
            if (rawValue == null) {
                return Optional.empty();
            }
            Map<?, ?> entry = objectMapper.readValue(rawValue.toString(), Map.class);
            String calculatedAtStr = (String) entry.get("calculatedAt");
            if (calculatedAtStr == null) {
                return Optional.empty();
            }
            return Optional.of(LocalDateTime.parse(calculatedAtStr));
        } catch (Exception e) {
            log.warn("ETA 캐시 조회 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public String throttleKey(Long appointmentId, Long memberId) {
        return THROTTLE_KEY_PREFIX + appointmentId + ":" + memberId;
    }
}
