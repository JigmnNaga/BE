package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.AppointmentLeaveRequest;
import com.jigmnnaga.application.dto.request.HostDelegateRequest;
import com.jigmnnaga.application.usecase.ParticipantManageUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Participant", description = "약속방 참여자 관리 관련 API")
@RestController
@RequestMapping("/api/v1/appointments/{appointmentId}")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantManageUseCase participantManageUseCase;

    @Operation(summary = "약속방 나가기", description = "약속방을 나갑니다. 내 일정 선택과 참석 응답은 삭제되며, 응답 기한 내에는 초대 링크로 다시 참여할 수 있습니다. 방장은 newHostMemberId로 새 방장을 지정해야 하며, 혼자 남은 경우 나갈 수 없습니다.")
    @PostMapping("/leave")
    public CommonResponse<Void> leaveAppointment(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId,
            @RequestBody(required = false) @Valid AppointmentLeaveRequest request
    ) {
        participantManageUseCase.leaveAppointment(memberId, appointmentId, request);
        return CommonResponse.success(ResponseMessage.APPOINTMENT_LEAVE_SUCCESS);
    }

    @Operation(summary = "참여자 내보내기", description = "방장이 참여자를 내보냅니다. 내보낸 멤버의 일정 선택은 삭제되며, 초대 링크로 다시 참여할 수 없습니다.")
    @PostMapping("/participants/{targetMemberId}/kick")
    public CommonResponse<Void> kickParticipant(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId,
            @PathVariable Long targetMemberId
    ) {
        participantManageUseCase.kickParticipant(memberId, appointmentId, targetMemberId);
        return CommonResponse.success(ResponseMessage.PARTICIPANT_KICK_SUCCESS);
    }

    @Operation(summary = "방장 위임", description = "방장이 약속에 참여 중인 다른 멤버에게 방장을 넘깁니다.")
    @PatchMapping("/host")
    public CommonResponse<Void> delegateHost(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId,
            @RequestBody @Valid HostDelegateRequest request
    ) {
        participantManageUseCase.delegateHost(memberId, appointmentId, request);
        return CommonResponse.success(ResponseMessage.HOST_DELEGATE_SUCCESS);
    }
}
