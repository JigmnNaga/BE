package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.AppointmentCreateRequest;
import com.jigmnnaga.application.dto.response.AppointmentCreateResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.AppointmentMapper;
import com.jigmnnaga.application.validator.AppointmentValidator;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.service.AppointmentGetService;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.domain.service.AppointmentSaveService;
import com.jigmnnaga.domain.service.MemberGetService;
import com.jigmnnaga.global.config.InviteProperties;
import com.jigmnnaga.global.util.InviteCodeGenerator;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(InviteProperties.class)
public class AppointmentCreateUseCase {

    private static final int MAX_INVITE_CODE_ATTEMPTS = 5;

    private final MemberGetService memberGetService;
    private final AppointmentGetService appointmentGetService;
    private final AppointmentSaveService appointmentSaveService;
    private final AppointmentMapper appointmentMapper;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final InviteProperties inviteProperties;
    private final AppointmentValidator appointmentValidator;
    private final AppointmentParticipantGetService appointmentParticipantGetService;

    @Transactional
    public AppointmentCreateResponse createAppointment(Long memberId, AppointmentCreateRequest request) {
        Member host = memberGetService.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<LocalDate> sortedDates = appointmentValidator.validateAndSortCandidateDates(
                request.dateSelectionType(), request.candidateDates());
        appointmentValidator.validateResponseDeadline(request.responseDeadline(), sortedDates.getFirst());

        List<Member> invitedMembers = getInvitedPreviousMembers(memberId, request.previousMembers());

        Appointment appointment = appointmentMapper.toAppointment(
                request, sortedDates, host, invitedMembers, generateInviteCode());
        appointmentSaveService.save(appointment);

        String inviteUrl = inviteProperties.inviteUrl(appointment.getInviteCode());
        return appointmentMapper.toCreateResponse(appointment, inviteUrl);
    }

    private List<Member> getInvitedPreviousMembers(Long memberId,
                                                   AppointmentCreateRequest.PreviousMembersRequest previousMembers) {
        if (previousMembers == null) {
            return List.of();
        }

        Long previousAppointmentId = previousMembers.appointmentId();
        appointmentParticipantGetService.findAcceptedByAppointmentIdAndMemberId(previousAppointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));

        Map<Long, Member> previousMemberById = appointmentParticipantGetService
                .findAllAcceptedWithMemberByAppointmentId(previousAppointmentId).stream()
                .map(AppointmentParticipant::getMember)
                .filter(member -> !member.getId().equals(memberId))
                .collect(Collectors.toMap(Member::getId, Function.identity()));

        return previousMembers.memberIds().stream()
                .distinct()
                .map(invitedMemberId -> Optional.ofNullable(previousMemberById.get(invitedMemberId))
                        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PREVIOUS_MEMBER)))
                .toList();
    }

    private String generateInviteCode() {
        for (int i = 0; i < MAX_INVITE_CODE_ATTEMPTS; i++) {
            String inviteCode = inviteCodeGenerator.generate();
            if (!appointmentGetService.existsByInviteCode(inviteCode)) {
                return inviteCode;
            }
        }
        throw new BusinessException(ErrorCode.INVITE_CODE_GENERATION_FAILED);
    }
}
