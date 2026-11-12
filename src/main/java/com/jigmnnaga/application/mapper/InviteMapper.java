package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.response.InviteInfoResponse;
import com.jigmnnaga.application.dto.response.ParticipantResponse;
import com.jigmnnaga.application.dto.response.ReceivedInviteResponse;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.ParticipantRole;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InviteMapper {

    private final ProfileImageUrlResolver profileImageUrlResolver;

    public InviteInfoResponse toInviteInfoResponse(Appointment appointment, List<AppointmentParticipant> participants,
                                                   Long memberId, boolean expired) {
        String hostNickname = participants.stream()
                .filter(AppointmentParticipant::isHost)
                .map(participant -> participant.getMember().getNickname())
                .findFirst()
                .orElse(null);

        Optional<AppointmentParticipant> me = participants.stream()
                .filter(participant -> participant.getMember().getId().equals(memberId))
                .findFirst();
        ParticipationStatus myStatus = me.map(AppointmentParticipant::getStatus).orElse(ParticipationStatus.PENDING);
        ParticipantRole myRole = me.map(AppointmentParticipant::getRole).orElse(ParticipantRole.MEMBER);

        List<AppointmentParticipant> visibleParticipants = participants.stream()
                .filter(participant -> !participant.isKicked())
                .toList();

        int respondedCount = (int) visibleParticipants.stream()
                .filter(participant -> !participant.isHost() && participant.hasResponded())
                .count();

        int joinedCount = (int) visibleParticipants.stream()
                .filter(participant -> participant.getStatus() == ParticipationStatus.ACCEPTED)
                .count();

        List<ParticipantResponse> participantResponses = visibleParticipants.stream()
                .sorted(Comparator.comparing((AppointmentParticipant participant) -> !participant.isHost()))
                .map(this::toParticipantResponse)
                .toList();

        return InviteInfoResponse.builder()
                .appointmentId(appointment.getId())
                .title(appointment.getTitle())
                .canceled(appointment.isCanceled())
                .hostNickname(hostNickname)
                .responseDeadline(appointment.getResponseDeadline())
                .expired(expired)
                .respondedCount(respondedCount)
                .joinedCount(joinedCount)
                .myStatus(myStatus)
                .myRole(myRole)
                .participants(participantResponses)
                .build();
    }

    public ReceivedInviteResponse toReceivedInviteResponse(Appointment appointment) {
        return ReceivedInviteResponse.builder()
                .appointmentId(appointment.getId())
                .title(appointment.getTitle())
                .hostNickname(appointment.getHostNickname().orElse(null))
                .inviteCode(appointment.getInviteCode())
                .candidateStartDate(appointment.getFirstCandidateDate())
                .candidateEndDate(appointment.getLastCandidateDate())
                .responseDeadline(appointment.getResponseDeadline())
                .build();
    }

    private ParticipantResponse toParticipantResponse(AppointmentParticipant participant) {
        Member member = participant.getMember();
        return ParticipantResponse.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .profileImageUrl(profileImageUrlResolver.resolve(member))
                .role(participant.getRole())
                .status(participant.getStatus())
                .build();
    }
}
