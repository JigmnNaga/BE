package com.jigmnnaga.application.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record ScheduleStatusResponse(
        int totalCount,
        int submittedCount,
        List<MemberScheduleResponse> members,
        List<LocationVoteResponse> locationVotes
) {

    @Builder
    public record MemberScheduleResponse(
            Long memberId,
            String nickname,
            String profileImageUrl,
            boolean submitted,
            List<AvailabilityResponse> availabilities,
            Long preferredLocationId
    ) {
    }

    @Builder
    public record LocationVoteResponse(
            Long locationId,
            String placeName,
            String address,
            String lineNames,
            int voteCount,
            boolean topVoted,
            List<VoterResponse> voters
    ) {
    }

    @Builder
    public record VoterResponse(
            Long memberId,
            String nickname,
            String profileImageUrl
    ) {
    }
}
