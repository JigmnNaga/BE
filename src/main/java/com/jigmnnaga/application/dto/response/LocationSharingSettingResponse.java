package com.jigmnnaga.application.dto.response;

import com.jigmnnaga.domain.entity.LocationSharingScope;
import lombok.Builder;

@Builder
public record LocationSharingSettingResponse(
        boolean enabled,
        LocationSharingScope scope,
        int startMinutesBefore
) {
}
