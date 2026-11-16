package com.jigmnnaga.global.client.google;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google")
public record GoogleProperties(
        List<String> clientIds
) {

    public List<String> validClientIds() {
        if (clientIds == null) {
            return List.of();
        }
        return clientIds.stream()
                .map(String::strip)
                .filter(clientId -> !clientId.isEmpty())
                .toList();
    }
}
