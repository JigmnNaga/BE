package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.AppointmentCreateRequest;
import com.jigmnnaga.application.dto.response.AppointmentCreateResponse;
import com.jigmnnaga.application.usecase.AppointmentCreateUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
}
