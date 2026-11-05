package com.jigmnnaga.global.config;

import com.jigmnnaga.global.util.TimeZones;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "seoulDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider seoulDateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now(TimeZones.SEOUL));
    }
}
