package com.jigmnnaga.application.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record TransitRouteRequest(

        @NotNull(message = "출발지 경도는 필수입니다.")
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double startLongitude,

        @NotNull(message = "출발지 위도는 필수입니다.")
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double startLatitude,

        @NotNull(message = "도착지 경도는 필수입니다.")
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double endLongitude,

        @NotNull(message = "도착지 위도는 필수입니다.")
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double endLatitude

) {}