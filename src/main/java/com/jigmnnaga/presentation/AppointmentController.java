package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.AppointmentCreateRequest;
import com.jigmnnaga.application.dto.response.AppointmentCreateResponse;
import com.jigmnnaga.application.dto.response.AppointmentDetailResponse;
import com.jigmnnaga.application.usecase.AppointmentCreateUseCase;
import com.jigmnnaga.application.usecase.AppointmentDetailGetUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Appointment", description = "약속방 관련 API")
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentCreateUseCase appointmentCreateUseCase;
    private final AppointmentDetailGetUseCase appointmentDetailGetUseCase;

    @Operation(summary = "약속방 생성", description = "약속 이름, 후보 날짜, 후보 위치(최대 5개), 응답 기한으로 약속방을 생성하고 초대 링크를 발급합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<AppointmentCreateResponse> createAppointment(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @RequestBody @Valid AppointmentCreateRequest request
    ) {
        AppointmentCreateResponse response = appointmentCreateUseCase.createAppointment(memberId, request);
        return CommonResponse.success(ResponseMessage.APPOINTMENT_CREATE_SUCCESS, response);
    }

    @Operation(summary = "약속 상세 조회", description = "약속 정보, 후보 날짜·장소, 응답 기한, 초대 링크, 내 역할, 참여 멤버와 일정 선택 여부를 조회합니다.")
    @GetMapping("/{appointmentId}")
    public CommonResponse<AppointmentDetailResponse> getAppointmentDetail(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId
    ) {
        AppointmentDetailResponse response = appointmentDetailGetUseCase.getAppointmentDetail(memberId, appointmentId);
        return CommonResponse.success(ResponseMessage.APPOINTMENT_DETAIL_GET_SUCCESS, response);
    }
}
