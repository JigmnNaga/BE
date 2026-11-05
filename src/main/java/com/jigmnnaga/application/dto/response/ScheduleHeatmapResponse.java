package com.jigmnnaga.application.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record ScheduleHeatmapResponse(
        int slotMinutes,
        int totalMemberCount,
        List<DateSlotsResponse> dates,
        List<TimeRangeCandidateResponse> topCandidates
) {

    @Builder
    public record DateSlotsResponse(
            LocalDate date,
            List<SlotResponse> slots
    ) {
    }

    @Builder
    public record SlotResponse(
            String startTime,
            String endTime,
            int availableCount,
            List<HeatmapMemberResponse> members
    ) {
    }

    @Builder
    public record TimeRangeCandidateResponse(
            LocalDate date,
            String startTime,
            String endTime,
            int availableCount,
            List<HeatmapMemberResponse> members
    ) {
    }

    @Builder
    public record HeatmapMemberResponse(
            Long memberId,
            String nickname,
            String profileImageUrl
    ) {
    }
}
