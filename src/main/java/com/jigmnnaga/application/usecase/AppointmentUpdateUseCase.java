package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.AppointmentTitleUpdateRequest;
import com.jigmnnaga.application.dto.request.CandidateDatesUpdateRequest;
import com.jigmnnaga.application.dto.request.CandidateLocationsUpdateRequest;
import com.jigmnnaga.application.dto.request.ResponseDeadlineUpdateRequest;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.AppointmentMapper;
import com.jigmnnaga.application.validator.AppointmentAccessValidator;
import com.jigmnnaga.application.validator.AppointmentValidator;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentCandidateLocation;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AppointmentUpdateUseCase {

    private final AppointmentAccessValidator appointmentAccessValidator;
    private final AppointmentValidator appointmentValidator;
    private final AppointmentMapper appointmentMapper;

    @Transactional
    public void updateTitle(Long memberId, Long appointmentId, AppointmentTitleUpdateRequest request) {
        Appointment appointment = getEditableAppointment(memberId, appointmentId);
        appointment.updateTitle(request.title().strip());
    }

    @Transactional
    public void updateCandidateDates(Long memberId, Long appointmentId, CandidateDatesUpdateRequest request) {
        Appointment appointment = getEditableAppointment(memberId, appointmentId);

        List<LocalDate> sortedDates = appointmentValidator.validateAndSortCandidateDates(
                request.dateSelectionType(), request.candidateDates());
        appointmentValidator.validateCandidateDatesAfterDeadline(appointment.getResponseDeadline(), sortedDates.getFirst());

        appointment.updateCandidateDates(request.dateSelectionType(), sortedDates);
    }

    @Transactional
    public void updateCandidateLocations(Long memberId, Long appointmentId, CandidateLocationsUpdateRequest request) {
        Appointment appointment = getEditableAppointment(memberId, appointmentId);

        Set<Long> keepLocationIds = Set.copyOf(request.keepLocationIds());
        Set<Long> currentLocationIds = appointment.getCandidateLocations().stream()
                .map(AppointmentCandidateLocation::getId)
                .collect(Collectors.toSet());
        if (!currentLocationIds.containsAll(keepLocationIds)) {
            throw new BusinessException(ErrorCode.INVALID_CANDIDATE_LOCATION);
        }
        appointmentValidator.validateCandidateLocationCount(keepLocationIds.size() + request.newLocations().size());

        appointment.updateCandidateLocations(keepLocationIds,
                appointmentMapper.toCandidateLocations(request.newLocations()));
    }

    @Transactional
    public void updateResponseDeadline(Long memberId, Long appointmentId, ResponseDeadlineUpdateRequest request) {
        Appointment appointment = getEditableAppointment(memberId, appointmentId);

        appointmentValidator.validateResponseDeadline(request.responseDeadline(), appointment.getFirstCandidateDate());
        appointment.updateResponseDeadline(request.responseDeadline());
    }

    private Appointment getEditableAppointment(Long memberId, Long appointmentId) {
        Appointment appointment = appointmentAccessValidator.getHostAppointment(memberId, appointmentId);
        appointmentAccessValidator.validateNotCanceled(appointment);
        return appointment;
    }
}
