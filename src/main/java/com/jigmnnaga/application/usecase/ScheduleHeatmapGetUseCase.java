package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.response.ScheduleHeatmapResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.ScheduleHeatmapMapper;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ScheduleHeatmapGetUseCase {

    private static final Set<Integer> ALLOWED_SLOT_MINUTES = Set.of(30, 60);

    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final ScheduleHeatmapMapper scheduleHeatmapMapper;

    @Transactional(readOnly = true)
    public ScheduleHeatmapResponse getHeatmap(Long memberId, Long appointmentId, int slotMinutes) {
        if (!ALLOWED_SLOT_MINUTES.contains(slotMinutes)) {
            throw new BusinessException(ErrorCode.INVALID_SLOT_MINUTES);
        }

        AppointmentParticipant me = appointmentParticipantGetService
                .findAcceptedByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));
        List<AppointmentParticipant> participants =
                appointmentParticipantGetService.findAllAcceptedWithMemberByAppointmentId(appointmentId);

        return scheduleHeatmapMapper.toHeatmapResponse(me.getAppointment(), participants, slotMinutes);
    }
}
