package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.response.MyScheduleResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.ScheduleMapper;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ScheduleGetUseCase {

    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final ScheduleMapper scheduleMapper;

    @Transactional(readOnly = true)
    public MyScheduleResponse getMySchedule(Long memberId, Long appointmentId) {
        AppointmentParticipant participant = appointmentParticipantGetService
                .findAcceptedByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));

        return scheduleMapper.toMyScheduleResponse(participant);
    }
}
