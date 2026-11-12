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
public class LatenessCache {

    private static final Duration RESULT_TTL = Duration.ofMinutes(10);

    /**
     * Hash 키: "lateness:{appointmentId}" → field: "{memberId}" → JSON
     * 지각 예측 결과를 저장합니다.
     */
    private static final String RESULT_KEY_PREFIX = "lateness:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 지각 예측 결과를 저장합니다. calculatedAt이 기존 항목보다 최신일 때만 덮어씁니다.
     * 오래된 계산 결과가 최신 결과를 덮어쓰는 것을 방지합니다.
     *
     * @return true: 저장됨, false: 오래된 결과이므로 건너뜀
     */
    public boolean putIfNewer(Long appointmentId, Long memberId,
                               int expectedLatenessMinutes, boolean isExpectedLate,
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
                    "expectedLatenessMinutes", String.valueOf(expectedLatenessMinutes),
                    "isExpectedLate", String.valueOf(isExpectedLate),
                    "calculatedAt", calculatedAt.toString()
            );
            redisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(entry));
            redisTemplate.expire(key, RESULT_TTL);
            return true;
        } catch (Exception e) {
            log.warn("지각 캐시 저장 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 저장된 지각 예측 정보를 반환합니다.
     */
    public Optional<LatenessInfo> get(Long appointmentId, Long memberId) {
        String key = RESULT_KEY_PREFIX + appointmentId;
        String field = String.valueOf(memberId);
        try {
            Object rawValue = redisTemplate.opsForHash().get(key, field);
            if (rawValue == null) {
                return Optional.empty();
            }
            Map<?, ?> entry = objectMapper.readValue(rawValue.toString(), Map.class);
            String latenessMinutesStr = (String) entry.get("expectedLatenessMinutes");
            String isLateStr = (String) entry.get("isExpectedLate");
            String calculatedAtStr = (String) entry.get("calculatedAt");
            if (latenessMinutesStr == null || isLateStr == null || calculatedAtStr == null) {
                return Optional.empty();
            }
            return Optional.of(new LatenessInfo(
                    Integer.parseInt(latenessMinutesStr),
                    Boolean.parseBoolean(isLateStr),
                    LocalDateTime.parse(calculatedAtStr)
            ));
        } catch (Exception e) {
            log.warn("지각 캐시 조회 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public record LatenessInfo(int expectedLatenessMinutes, boolean isExpectedLate, LocalDateTime calculatedAt) {
    }
}
