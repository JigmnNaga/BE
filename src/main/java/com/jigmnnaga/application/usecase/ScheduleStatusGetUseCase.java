package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.response.ScheduleStatusResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.ScheduleMapper;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ScheduleStatusGetUseCase {

    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final ScheduleMapper scheduleMapper;

    @Transactional(readOnly = true)
    public ScheduleStatusResponse getScheduleStatus(Long memberId, Long appointmentId) {
        AppointmentParticipant me = appointmentParticipantGetService
                .findAcceptedByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));

        List<AppointmentParticipant> participants =
                appointmentParticipantGetService.findAllAcceptedWithMemberByAppointmentId(appointmentId);

        return scheduleMapper.toScheduleStatusResponse(me.getAppointment(), participants);
    }
}
