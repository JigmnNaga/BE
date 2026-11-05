package com.jigmnnaga.presentation;

import com.jigmnnaga.application.usecase.MemberWithdrawUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "회원 관련 API")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberWithdrawUseCase memberWithdrawUseCase;

    @Operation(summary = "회원 탈퇴", description = "회원 정보를 익명화하고 카카오 연결을 끊습니다. 응답 대기 중인 초대와 다가오는 약속 참여는 삭제됩니다. 다가오는 약속의 방장인 경우 참여자가 2명 이하면 약속방을 삭제하고, 3명 이상이면 가장 먼저 참여한 멤버에게 방장을 위임합니다. 지난 약속·취소된 약속의 기록은 '탈퇴한 사용자'로 보존됩니다.")
    @DeleteMapping("/me")
    public CommonResponse<Void> withdraw(@Parameter(hidden = true) @LoginMember Long memberId) {
        memberWithdrawUseCase.withdraw(memberId);
        return CommonResponse.success(ResponseMessage.WITHDRAW_SUCCESS);
    }
}
