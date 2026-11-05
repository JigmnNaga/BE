package com.jigmnnaga.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisConfigTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void redis_저장_조회_TTL_테스트() {
        String key = "test:redis:connection";

        try {
            // 1. Redis에 데이터 저장 (TTL 60초)
            redisTemplate.opsForValue()
                    .set(key, "success", Duration.ofSeconds(60));

            // 2. 저장한 데이터 조회
            String value = redisTemplate.opsForValue().get(key);

            assertThat(value).isEqualTo("success");

            // 3. TTL 확인
            Long ttl = redisTemplate.getExpire(key);

            assertThat(ttl).isNotNull();
            assertThat(ttl).isPositive();
            assertThat(ttl).isLessThanOrEqualTo(60L);

        } finally {
            // 4. 테스트 데이터 삭제
            redisTemplate.delete(key);
        }
    }
}