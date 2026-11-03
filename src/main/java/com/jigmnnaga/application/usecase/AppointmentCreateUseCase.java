package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.AppointmentCreateRequest;
import com.jigmnnaga.application.dto.response.AppointmentCreateResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.AppointmentMapper;
import com.jigmnnaga.application.validator.AppointmentValidator;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.service.AppointmentGetService;
import com.jigmnnaga.domain.service.AppointmentSaveService;
import com.jigmnnaga.domain.service.MemberGetService;
import com.jigmnnaga.global.config.InviteProperties;
import com.jigmnnaga.global.util.InviteCodeGenerator;
import java.time.LocalDate;
import java.util.List;
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

    @Transactional
    public AppointmentCreateResponse createAppointment(Long memberId, AppointmentCreateRequest request) {
        Member host = memberGetService.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<LocalDate> sortedDates = appointmentValidator.validateAndSortCandidateDates(
                request.dateSelectionType(), request.candidateDates());
        appointmentValidator.validateResponseDeadline(request.responseDeadline(), sortedDates.getFirst());

        Appointment appointment = appointmentMapper.toAppointment(request, sortedDates, host, generateInviteCode());
        appointmentSaveService.save(appointment);

        String inviteUrl = inviteProperties.inviteUrl(appointment.getInviteCode());
        return appointmentMapper.toCreateResponse(appointment, inviteUrl);
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
