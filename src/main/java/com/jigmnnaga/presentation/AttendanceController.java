package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.AttendanceRespondRequest;
import com.jigmnnaga.application.usecase.AttendanceRespondUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Attendance", description = "확정 약속 참석 응답 관련 API")
@RestController
@RequestMapping("/api/v1/appointments/{appointmentId}/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceRespondUseCase attendanceRespondUseCase;

    @Operation(summary = "참석 응답 등록/변경", description = "확정된 약속에 참석 여부를 응답합니다. 약속 시간이 지나기 전까지 변경할 수 있습니다.")
    @PutMapping("/me")
    public CommonResponse<Void> respondAttendance(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId,
            @RequestBody @Valid AttendanceRespondRequest request
    ) {
        attendanceRespondUseCase.respondAttendance(memberId, appointmentId, request);
        return CommonResponse.success(ResponseMessage.ATTENDANCE_RESPOND_SUCCESS);
    }
}
