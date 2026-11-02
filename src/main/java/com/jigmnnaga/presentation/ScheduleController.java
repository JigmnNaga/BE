package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.ScheduleSubmitRequest;
import com.jigmnnaga.application.dto.response.MyScheduleResponse;
import com.jigmnnaga.application.dto.response.ScheduleStatusResponse;
import com.jigmnnaga.application.usecase.ScheduleGetUseCase;
import com.jigmnnaga.application.usecase.ScheduleStatusGetUseCase;
import com.jigmnnaga.application.usecase.ScheduleSubmitUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Schedule", description = "약속 일정 선택 관련 API")
@RestController
@RequestMapping("/api/v1/appointments/{appointmentId}/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleSubmitUseCase scheduleSubmitUseCase;
    private final ScheduleGetUseCase scheduleGetUseCase;
    private final ScheduleStatusGetUseCase scheduleStatusGetUseCase;

    @Operation(summary = "일정 선택 제출/수정", description = "후보 날짜 중 가능한 날짜별 시간과 희망 장소를 제출합니다. 기존 선택은 전체 교체됩니다.")
    @PutMapping("/me")
    public CommonResponse<Void> submitSchedule(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId,
            @RequestBody @Valid ScheduleSubmitRequest request
    ) {
        scheduleSubmitUseCase.submitSchedule(memberId, appointmentId, request);
        return CommonResponse.success(ResponseMessage.SCHEDULE_SUBMIT_SUCCESS);
    }

    @Operation(summary = "내 일정 선택 조회", description = "내가 제출한 날짜별 가능 시간과 희망 장소를 조회합니다. 수정 화면 초기값으로 사용합니다.")
    @GetMapping("/me")
    public CommonResponse<MyScheduleResponse> getMySchedule(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId
    ) {
        MyScheduleResponse response = scheduleGetUseCase.getMySchedule(memberId, appointmentId);
        return CommonResponse.success(ResponseMessage.SCHEDULE_GET_SUCCESS, response);
    }

    @Operation(summary = "일정 응답 현황 조회", description = "참여 멤버 전체의 날짜별 가능 시간, 희망 장소, 장소별 선택 인원, 선택 완료 인원을 조회합니다. 시간표와 장소 선택 현황 화면에 사용합니다.")
    @GetMapping
    public CommonResponse<ScheduleStatusResponse> getScheduleStatus(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId
    ) {
        ScheduleStatusResponse response = scheduleStatusGetUseCase.getScheduleStatus(memberId, appointmentId);
        return CommonResponse.success(ResponseMessage.SCHEDULE_STATUS_GET_SUCCESS, response);
    }
}
