package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.AppointmentListType;
import com.jigmnnaga.application.dto.response.CalendarResponse;
import com.jigmnnaga.application.dto.response.MyAppointmentResponse;
import com.jigmnnaga.application.usecase.MyAppointmentGetUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "My Appointment", description = "내 약속 목록·캘린더 관련 API")
@RestController
@RequestMapping("/api/v1/appointments/me")
@RequiredArgsConstructor
public class MyAppointmentController {

    private final MyAppointmentGetUseCase myAppointmentGetUseCase;

    @Operation(summary = "내 약속 목록 조회", description = "참여 중인 약속을 다가오는 약속(UPCOMING, 가까운 순) 또는 지난 약속(PAST, 최근 순)으로 조회합니다.")
    @GetMapping
    public CommonResponse<List<MyAppointmentResponse>> getMyAppointments(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @Parameter(description = "UPCOMING: 다가오는 약속, PAST: 지난 약속", example = "UPCOMING")
            @RequestParam AppointmentListType type
    ) {
        List<MyAppointmentResponse> response = myAppointmentGetUseCase.getMyAppointments(memberId, type);
        return CommonResponse.success(ResponseMessage.MY_APPOINTMENTS_GET_SUCCESS, response);
    }

    @Operation(summary = "다음 약속 조회", description = "날짜·시간이 확정된 다가오는 약속 중 가장 가까운 약속을 조회합니다. 없으면 data가 null입니다.")
    @GetMapping("/next")
    public CommonResponse<MyAppointmentResponse> getNextAppointment(
            @Parameter(hidden = true) @LoginMember Long memberId
    ) {
        MyAppointmentResponse response = myAppointmentGetUseCase.getNextAppointment(memberId);
        return CommonResponse.success(ResponseMessage.NEXT_APPOINTMENT_GET_SUCCESS, response);
    }

    @Operation(summary = "월별 캘린더 조회", description = "해당 월에 날짜가 확정된 내 약속을 날짜별로 조회합니다.")
    @GetMapping("/calendar")
    public CommonResponse<CalendarResponse> getCalendar(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @Parameter(description = "조회할 월 (yyyy-MM)", example = "2026-09")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        CalendarResponse response = myAppointmentGetUseCase.getCalendar(memberId, month);
        return CommonResponse.success(ResponseMessage.CALENDAR_GET_SUCCESS, response);
    }
}
