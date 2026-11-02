package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.response.InviteAcceptResponse;
import com.jigmnnaga.application.dto.response.InviteInfoResponse;
import com.jigmnnaga.application.usecase.InviteGetUseCase;
import com.jigmnnaga.application.usecase.InviteRespondUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Invite", description = "약속 초대 관련 API")
@RestController
@RequestMapping("/api/v1/invites/{inviteCode}")
@RequiredArgsConstructor
public class InviteController {

    private final InviteGetUseCase inviteGetUseCase;
    private final InviteRespondUseCase inviteRespondUseCase;

    @Operation(summary = "초대 정보 조회", description = "초대 코드로 약속 이름, 방장, 응답 인원, 참여 멤버 현황과 내 응답 상태를 조회합니다.")
    @GetMapping
    public CommonResponse<InviteInfoResponse> getInvite(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable String inviteCode
    ) {
        InviteInfoResponse response = inviteGetUseCase.getInvite(memberId, inviteCode);
        return CommonResponse.success(ResponseMessage.INVITE_GET_SUCCESS, response);
    }

    @Operation(summary = "초대 수락", description = "초대를 수락하고 약속방에 참여합니다. 응답 기한이 지나면 수락할 수 없습니다.")
    @PostMapping("/accept")
    public CommonResponse<InviteAcceptResponse> acceptInvite(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable String inviteCode
    ) {
        InviteAcceptResponse response = inviteRespondUseCase.acceptInvite(memberId, inviteCode);
        return CommonResponse.success(ResponseMessage.INVITE_ACCEPT_SUCCESS, response);
    }

    @Operation(summary = "초대 거절", description = "초대를 거절합니다. 응답 기한이 지나면 거절할 수 없습니다.")
    @PostMapping("/decline")
    public CommonResponse<Void> declineInvite(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable String inviteCode
    ) {
        inviteRespondUseCase.declineInvite(memberId, inviteCode);
        return CommonResponse.success(ResponseMessage.INVITE_DECLINE_SUCCESS);
    }
}
