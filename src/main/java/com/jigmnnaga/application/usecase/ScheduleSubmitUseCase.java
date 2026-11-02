package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.ScheduleSubmitRequest;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.ScheduleMapper;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentCandidateDate;
import com.jigmnnaga.domain.entity.AppointmentCandidateLocation;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ScheduleSubmitUseCase {

    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final ScheduleMapper scheduleMapper;

    @Transactional
    public void submitSchedule(Long memberId, Long appointmentId, ScheduleSubmitRequest request) {
        AppointmentParticipant participant = appointmentParticipantGetService
                .findAcceptedByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));
        Appointment appointment = participant.getAppointment();

        validateAvailabilities(appointment, request.availabilities());
        AppointmentCandidateLocation preferredLocation = getPreferredLocation(appointment, request.preferredLocationId());

        participant.updateSchedule(scheduleMapper.toAvailabilities(request), preferredLocation);
    }

    private void validateAvailabilities(Appointment appointment,
                                        List<ScheduleSubmitRequest.AvailabilityRequest> availabilities) {
        Set<LocalDate> candidateDates = appointment.getCandidateDates().stream()
                .map(AppointmentCandidateDate::getCandidateDate)
                .collect(Collectors.toSet());
        Set<LocalDate> selectedDates = new HashSet<>();

        for (ScheduleSubmitRequest.AvailabilityRequest availability : availabilities) {
            if (!candidateDates.contains(availability.date())) {
                throw new BusinessException(ErrorCode.INVALID_AVAILABLE_DATE);
            }
            if (!selectedDates.add(availability.date())) {
                throw new BusinessException(ErrorCode.DUPLICATE_AVAILABLE_DATE);
            }
            if (!availability.startTime().isBefore(availability.endTime())) {
                throw new BusinessException(ErrorCode.INVALID_AVAILABLE_TIME);
            }
        }
    }

    private AppointmentCandidateLocation getPreferredLocation(Appointment appointment, Long preferredLocationId) {
        return appointment.getCandidateLocations().stream()
                .filter(location -> location.getId().equals(preferredLocationId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PREFERRED_LOCATION));
    }
}
