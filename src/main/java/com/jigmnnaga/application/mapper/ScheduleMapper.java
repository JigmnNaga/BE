package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.request.ScheduleSubmitRequest;
import com.jigmnnaga.application.dto.response.MyScheduleResponse;
import com.jigmnnaga.domain.entity.AppointmentCandidateLocation;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.ParticipantAvailability;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ScheduleMapper {

    public List<ParticipantAvailability> toAvailabilities(ScheduleSubmitRequest request) {
        return request.availabilities().stream()
                .map(availability -> ParticipantAvailability.builder()
                        .availableDate(availability.date())
                        .startTime(availability.startTime())
                        .endTime(availability.endTime())
                        .build())
                .toList();
    }

    public MyScheduleResponse toMyScheduleResponse(AppointmentParticipant participant) {
        AppointmentCandidateLocation preferredLocation = participant.getPreferredLocation();

        return MyScheduleResponse.builder()
                .submitted(participant.hasSubmittedSchedule())
                .availabilities(participant.getAvailabilities().stream()
                        .sorted(Comparator.comparing(ParticipantAvailability::getAvailableDate))
                        .map(availability -> MyScheduleResponse.AvailabilityResponse.builder()
                                .date(availability.getAvailableDate())
                                .startTime(availability.getStartTime())
                                .endTime(availability.getEndTime())
                                .build())
                        .toList())
                .preferredLocationId(preferredLocation == null ? null : preferredLocation.getId())
                .build();
    }
}
