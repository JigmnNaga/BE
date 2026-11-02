package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.AppointmentCreateRequest;
import com.jigmnnaga.application.dto.response.AppointmentCreateResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.AppointmentMapper;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.DateSelectionType;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.service.AppointmentGetService;
import com.jigmnnaga.domain.service.AppointmentSaveService;
import com.jigmnnaga.domain.service.MemberGetService;
import com.jigmnnaga.global.config.InviteProperties;
import com.jigmnnaga.global.util.InviteCodeGenerator;
import com.jigmnnaga.global.util.TimeZones;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Transactional
    public AppointmentCreateResponse createAppointment(Long memberId, AppointmentCreateRequest request) {
        Member host = memberGetService.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<LocalDate> sortedDates = request.candidateDates().stream()
                .distinct()
                .sorted()
                .toList();
        validateCandidateDates(request.dateSelectionType(), request.candidateDates().size(), sortedDates);
        validateResponseDeadline(request.responseDeadline(), sortedDates.getFirst());

        Appointment appointment = appointmentMapper.toAppointment(request, sortedDates, host, generateInviteCode());
        appointmentSaveService.save(appointment);

        String inviteUrl = inviteProperties.inviteUrl(appointment.getInviteCode());
        return appointmentMapper.toCreateResponse(appointment, inviteUrl);
    }

    private void validateCandidateDates(DateSelectionType type, int requestedSize, List<LocalDate> sortedDates) {
        if (requestedSize != sortedDates.size()) {
            throw new BusinessException(ErrorCode.DUPLICATE_CANDIDATE_DATE);
        }
        if (sortedDates.getFirst().isBefore(LocalDate.now(TimeZones.SEOUL))) {
            throw new BusinessException(ErrorCode.PAST_CANDIDATE_DATE);
        }

        boolean isValid = switch (type) {
            case SINGLE -> sortedDates.size() == 1;
            case RANGE -> sortedDates.size() >= 2 && isConsecutive(sortedDates);
            case MULTIPLE -> sortedDates.size() >= 2;
        };
        if (!isValid) {
            throw new BusinessException(ErrorCode.INVALID_CANDIDATE_DATES);
        }
    }

    private boolean isConsecutive(List<LocalDate> sortedDates) {
        LocalDate first = sortedDates.getFirst();
        LocalDate last = sortedDates.getLast();
        return first.plusDays(sortedDates.size() - 1L).equals(last);
    }

    private void validateResponseDeadline(LocalDateTime responseDeadline, LocalDate firstCandidateDate) {
        if (!responseDeadline.isAfter(LocalDateTime.now(TimeZones.SEOUL))
                || !responseDeadline.isBefore(firstCandidateDate.atStartOfDay())) {
            throw new BusinessException(ErrorCode.INVALID_RESPONSE_DEADLINE);
        }
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
