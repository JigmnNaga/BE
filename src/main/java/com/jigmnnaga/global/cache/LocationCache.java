package com.jigmnnaga.global.cache;

import com.jigmnnaga.domain.entity.DepartureStatus;
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
public class LocationCache {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "location:appointment:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void put(Long appointmentId, Long memberId, double latitude, double longitude,
                    DepartureStatus departureStatus, LocalDateTime updatedAt) {
        String key = key(appointmentId);
        String field = field(memberId);
        try {
            Map<String, String> entry = Map.of(
                    "latitude", String.valueOf(latitude),
                    "longitude", String.valueOf(longitude),
                    "departureStatus", departureStatus.name(),
                    "updatedAt", updatedAt.toString()
            );
            redisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(entry));
            redisTemplate.expire(key, CACHE_TTL);
        } catch (Exception e) {
            log.warn("위치 캐시 저장 실패: {}", e.getMessage());
        }
    }

    public Optional<LocalDateTime> getLastUpdatedAt(Long appointmentId, Long memberId) {
        String key = key(appointmentId);
        String field = field(memberId);
        try {
            Object rawValue = redisTemplate.opsForHash().get(key, field);
            if (rawValue == null) {
                return Optional.empty();
            }
            Map<?, ?> entry = objectMapper.readValue(rawValue.toString(), Map.class);
            return Optional.of(LocalDateTime.parse((String) entry.get("updatedAt")));
        } catch (Exception e) {
            log.warn("위치 캐시 조회 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String key(Long appointmentId) {
        return KEY_PREFIX + appointmentId;
    }

    private String field(Long memberId) {
        return String.valueOf(memberId);
    }
}
