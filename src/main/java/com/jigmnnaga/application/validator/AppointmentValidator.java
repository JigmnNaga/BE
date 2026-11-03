package com.jigmnnaga.application.validator;

import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.entity.DateSelectionType;
import com.jigmnnaga.global.util.TimeZones;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AppointmentValidator {

    private static final int MAX_CANDIDATE_LOCATIONS = 5;

    public List<LocalDate> validateAndSortCandidateDates(DateSelectionType type, List<LocalDate> candidateDates) {
        List<LocalDate> sortedDates = candidateDates.stream()
                .distinct()
                .sorted()
                .toList();

        if (candidateDates.size() != sortedDates.size()) {
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
        return sortedDates;
    }

    public void validateResponseDeadline(LocalDateTime responseDeadline, LocalDate firstCandidateDate) {
        if (!responseDeadline.isAfter(LocalDateTime.now(TimeZones.SEOUL))
                || !responseDeadline.isBefore(firstCandidateDate.atStartOfDay())) {
            throw new BusinessException(ErrorCode.INVALID_RESPONSE_DEADLINE);
        }
    }

    public void validateCandidateDatesAfterDeadline(LocalDateTime responseDeadline, LocalDate firstCandidateDate) {
        if (!responseDeadline.isBefore(firstCandidateDate.atStartOfDay())) {
            throw new BusinessException(ErrorCode.CANDIDATE_DATE_BEFORE_RESPONSE_DEADLINE);
        }
    }

    public void validateCandidateLocationCount(int count) {
        if (count < 1 || count > MAX_CANDIDATE_LOCATIONS) {
            throw new BusinessException(ErrorCode.INVALID_CANDIDATE_LOCATION_COUNT);
        }
    }

    private boolean isConsecutive(List<LocalDate> sortedDates) {
        LocalDate first = sortedDates.getFirst();
        LocalDate last = sortedDates.getLast();
        return first.plusDays(sortedDates.size() - 1L).equals(last);
    }
}
