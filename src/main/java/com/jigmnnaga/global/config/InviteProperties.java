package com.jigmnnaga.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "invite")
public record InviteProperties(
        String baseUrl
) {
}
