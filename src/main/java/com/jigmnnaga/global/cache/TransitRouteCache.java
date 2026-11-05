
package com.jigmnnaga.global.cache;

import com.jigmnnaga.application.dto.request.TransitRouteRequest;
import com.jigmnnaga.application.dto.response.TransitRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransitRouteCache {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final String KEY_PREFIX = "transit:route:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<TransitRouteResponse> get(TransitRouteRequest request) {
        String key = createKey(request);

        try {
            String cached = redisTemplate.opsForValue().get(key);

            if (cached == null) {
                return Optional.empty();
            }

            return Optional.of(
                    objectMapper.readValue(cached, TransitRouteResponse.class)
            );
        } catch (Exception e) {
            log.warn("대중교통 경로 캐시 조회 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void put(TransitRouteRequest request, TransitRouteResponse response) {
        String key = createKey(request);

        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("대중교통 경로 캐시 저장 실패: {}", e.getMessage());
        }
    }

    private String createKey(TransitRouteRequest request) {
        return KEY_PREFIX
                + request.startLongitude() + ":"
                + request.startLatitude() + ":"
                + request.endLongitude() + ":"
                + request.endLatitude();
    }
}