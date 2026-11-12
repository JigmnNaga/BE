package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.request.ScheduleSubmitRequest;
import com.jigmnnaga.application.dto.response.AvailabilityResponse;
import com.jigmnnaga.application.dto.response.MyScheduleResponse;
import com.jigmnnaga.application.dto.response.ScheduleStatusResponse;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentCandidateLocation;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.ParticipantAvailability;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleMapper {

    private final ProfileImageUrlResolver profileImageUrlResolver;

    public List<ParticipantAvailability> toAvailabilities(ScheduleSubmitRequest request) {
        return request.availabilities().stream()
                .map(availability -> ParticipantAvailability.builder()
                        .availableDate(availability.date())
                        .startTime(availability.startTime())
                        .endTime(availability.endTime())
                        .build())
                .toList();
    }

    public MyScheduleResponse toMyScheduleResponse(AppointmentParticipant participant) {
        return MyScheduleResponse.builder()
                .submitted(participant.hasSubmittedSchedule())
                .availabilities(toAvailabilityResponses(participant))
                .preferredLocationId(getPreferredLocationId(participant))
                .build();
    }

    public ScheduleStatusResponse toScheduleStatusResponse(Appointment appointment,
                                                           List<AppointmentParticipant> participants) {
        List<ScheduleStatusResponse.MemberScheduleResponse> members = participants.stream()
                .sorted(Comparator.comparing((AppointmentParticipant participant) -> !participant.isHost()))
                .map(this::toMemberScheduleResponse)
                .toList();

        int topVoteCount = appointment.getCandidateLocations().stream()
                .mapToInt(location -> getVoters(location, participants).size())
                .max()
                .orElse(0);

        List<ScheduleStatusResponse.LocationVoteResponse> locationVotes = appointment.getCandidateLocations().stream()
                .map(location -> toLocationVoteResponse(location, participants, topVoteCount))
                .toList();

        int submittedCount = (int) participants.stream()
                .filter(AppointmentParticipant::hasSubmittedSchedule)
                .count();

        return ScheduleStatusResponse.builder()
                .totalCount(participants.size())
                .submittedCount(submittedCount)
                .members(members)
                .locationVotes(locationVotes)
                .build();
    }

    private ScheduleStatusResponse.MemberScheduleResponse toMemberScheduleResponse(AppointmentParticipant participant) {
        Member member = participant.getMember();
        return ScheduleStatusResponse.MemberScheduleResponse.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .profileImageUrl(profileImageUrlResolver.resolve(member))
                .submitted(participant.hasSubmittedSchedule())
                .availabilities(toAvailabilityResponses(participant))
                .preferredLocationId(getPreferredLocationId(participant))
                .build();
    }

    private ScheduleStatusResponse.LocationVoteResponse toLocationVoteResponse(AppointmentCandidateLocation location,
                                                                               List<AppointmentParticipant> participants,
                                                                               int topVoteCount) {
        List<Member> voters = getVoters(location, participants);

        return ScheduleStatusResponse.LocationVoteResponse.builder()
                .locationId(location.getId())
                .placeName(location.getPlaceName())
                .address(location.getAddress())
                .lineNames(location.getLineNames())
                .voteCount(voters.size())
                .topVoted(topVoteCount > 0 && voters.size() == topVoteCount)
                .voters(voters.stream()
                        .map(voter -> ScheduleStatusResponse.VoterResponse.builder()
                                .memberId(voter.getId())
                                .nickname(voter.getNickname())
                                .profileImageUrl(profileImageUrlResolver.resolve(voter))
                                .build())
                        .toList())
                .build();
    }

    private List<Member> getVoters(AppointmentCandidateLocation location, List<AppointmentParticipant> participants) {
        return participants.stream()
                .filter(participant -> Objects.equals(getPreferredLocationId(participant), location.getId()))
                .map(AppointmentParticipant::getMember)
                .toList();
    }

    private List<AvailabilityResponse> toAvailabilityResponses(AppointmentParticipant participant) {
        return participant.getAvailabilities().stream()
                .sorted(Comparator.comparing(ParticipantAvailability::getAvailableDate))
                .map(availability -> AvailabilityResponse.builder()
                        .date(availability.getAvailableDate())
                        .startTime(availability.getStartTime())
                        .endTime(availability.getEndTime())
                        .build())
                .toList();
    }

    private Long getPreferredLocationId(AppointmentParticipant participant) {
        AppointmentCandidateLocation preferredLocation = participant.getPreferredLocation();
        return preferredLocation == null ? null : preferredLocation.getId();
    }
}
