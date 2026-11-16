package com.jigmnnaga.application.dto.response;

import lombok.Builder;

@Builder
public record LocationSharingResponse(
        boolean enabled
) {
}
