package com.jigmnnaga.global.client.fcm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase")
public record FirebaseProperties(
        String credentialsBase64,
        String credentialsPath
) {
}
