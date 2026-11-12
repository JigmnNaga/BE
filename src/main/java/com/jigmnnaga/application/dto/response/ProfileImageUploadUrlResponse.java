package com.jigmnnaga.application.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ProfileImageUploadUrlResponse(
        String uploadUrl,
        String profileImageKey,
        LocalDateTime expiresAt
) {
}
