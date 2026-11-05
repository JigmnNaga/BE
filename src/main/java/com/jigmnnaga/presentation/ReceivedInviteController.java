package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.response.ReceivedInviteResponse;
import com.jigmnnaga.application.usecase.InviteGetUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Invite", description = "약속 초대 관련 API")
@RestController
@RequestMapping("/api/v1/invites/me")
@RequiredArgsConstructor
public class ReceivedInviteController {

    private final InviteGetUseCase inviteGetUseCase;

    @Operation(summary = "받은 초대 목록 조회", description = "이전 약속 멤버 불러오기로 바로 초대받아 응답 대기 중인 초대 목록을 조회합니다. 만료·취소된 초대는 제외하며, 응답 기한이 가까운 순으로 정렬합니다. 수락/거절은 inviteCode로 기존 초대 API를 사용합니다.")
    @GetMapping
    public CommonResponse<List<ReceivedInviteResponse>> getReceivedInvites(
            @Parameter(hidden = true) @LoginMember Long memberId
    ) {
        List<ReceivedInviteResponse> response = inviteGetUseCase.getReceivedInvites(memberId);
        return CommonResponse.success(ResponseMessage.MY_INVITES_GET_SUCCESS, response);
    }
}
