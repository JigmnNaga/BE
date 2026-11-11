package com.jigmnnaga.application.dto.response;

import com.jigmnnaga.domain.entity.TravelMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record TravelOptionsResponse(
        String destinationName,
        double destinationLatitude,
        double destinationLongitude,
        LocalDateTime appointmentDateTime,
        LocalDateTime departureTime,
        Boolean departureRequired,
        TravelMode myTravelMode,
        List<TravelOptionResponse> options
) {

    @Builder
    public record TravelOptionResponse(
            TravelMode mode,
            int durationMinutes,
            int distanceMeters,
            LocalDateTime estimatedArrivalTime,
            List<TravelStepResponse> steps,
            List<CoordinateResponse> path
    ) {
    }

    @Builder
    public record TravelStepResponse(
            String type,
            int minutes,
            String name
    ) {
    }

    @Builder
    public record CoordinateResponse(
            double latitude,
            double longitude
    ) {
    }
}
