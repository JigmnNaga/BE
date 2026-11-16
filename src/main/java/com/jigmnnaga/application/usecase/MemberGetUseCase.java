package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.response.AppointmentStatsResponse;
import com.jigmnnaga.application.dto.response.MyInfoResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.MemberMapper;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.AttendanceStatus;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.domain.service.MemberGetService;
import com.jigmnnaga.domain.service.MemberLocationGetService;
import com.jigmnnaga.global.util.TimeZones;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MemberGetUseCase {

    private final MemberGetService memberGetService;
    private final MemberLocationGetService memberLocationGetService;
    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final MemberMapper memberMapper;

    @Transactional(readOnly = true)
    public MyInfoResponse getMyInfo(Long memberId) {
        Member member = memberGetService.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return memberMapper.toMyInfoResponse(member, memberLocationGetService.findAllByMemberId(memberId));
    }

    @Transactional(readOnly = true)
    public AppointmentStatsResponse getAppointmentStats(Long memberId) {
        LocalDateTime now = LocalDateTime.now(TimeZones.SEOUL);
        List<AppointmentParticipant> attended =
                appointmentParticipantGetService.findAllAcceptedWithAppointmentByMemberId(memberId).stream()
                        .filter(participant -> participant.getAttendanceStatus() == AttendanceStatus.ATTENDING)
                        .filter(participant -> isPastConfirmedAppointment(participant.getAppointment(), now))
                        .toList();

        int totalCount = attended.size();
        int onTimeCount = (int) attended.stream().filter(AppointmentParticipant::arrivedOnTime).count();
        return AppointmentStatsResponse.builder()
                .totalCount(totalCount)
                .onTimeCount(onTimeCount)
                .lateCount(totalCount - onTimeCount)
                .onTimeRate(totalCount == 0 ? null : percentage(onTimeCount, totalCount))
                .build();
    }

    private boolean isPastConfirmedAppointment(Appointment appointment, LocalDateTime now) {
        return !appointment.isCanceled() && appointment.isConfirmed()
                && appointment.getConfirmedDateTime().isBefore(now);
    }

    private double percentage(int count, int totalCount) {
        return BigDecimal.valueOf(count * 100L)
                .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
