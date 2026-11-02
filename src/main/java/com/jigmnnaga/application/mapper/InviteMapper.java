package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.response.InviteInfoResponse;
import com.jigmnnaga.application.dto.response.ParticipantResponse;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InviteMapper {

    public InviteInfoResponse toInviteInfoResponse(Appointment appointment, List<AppointmentParticipant> participants,
                                                   Long memberId, boolean expired) {
        String hostNickname = participants.stream()
                .filter(AppointmentParticipant::isHost)
                .map(participant -> participant.getMember().getNickname())
                .findFirst()
                .orElse(null);

        ParticipationStatus myStatus = participants.stream()
                .filter(participant -> participant.getMember().getId().equals(memberId))
                .map(AppointmentParticipant::getStatus)
                .findFirst()
                .orElse(ParticipationStatus.PENDING);

        int respondedCount = (int) participants.stream()
                .filter(participant -> !participant.isHost() && participant.hasResponded())
                .count();

        int joinedCount = (int) participants.stream()
                .filter(participant -> participant.getStatus() == ParticipationStatus.ACCEPTED)
                .count();

        List<ParticipantResponse> participantResponses = participants.stream()
                .sorted(Comparator.comparing((AppointmentParticipant participant) -> !participant.isHost()))
                .map(this::toParticipantResponse)
                .toList();

        return InviteInfoResponse.builder()
                .appointmentId(appointment.getId())
                .title(appointment.getTitle())
                .hostNickname(hostNickname)
                .responseDeadline(appointment.getResponseDeadline())
                .expired(expired)
                .respondedCount(respondedCount)
                .joinedCount(joinedCount)
                .myStatus(myStatus)
                .participants(participantResponses)
                .build();
    }

    private ParticipantResponse toParticipantResponse(AppointmentParticipant participant) {
        Member member = participant.getMember();
        return ParticipantResponse.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .role(participant.getRole())
                .status(participant.getStatus())
                .build();
    }
}
