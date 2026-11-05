package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.ChatReadRequest;
import com.jigmnnaga.application.dto.response.ChatMessagePageResponse;
import com.jigmnnaga.application.dto.response.ChatReadResponse;
import com.jigmnnaga.application.usecase.ChatMessageGetUseCase;
import com.jigmnnaga.application.usecase.ChatReadUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat", description = "약속방 채팅 관련 API (실시간 송수신은 WebSocket STOMP: /ws)")
@RestController
@RequestMapping("/api/v1/appointments/{appointmentId}/messages")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageGetUseCase chatMessageGetUseCase;
    private final ChatReadUseCase chatReadUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    @Operation(summary = "이전 메시지 조회", description = "커서 기반으로 이전 메시지를 조회합니다. cursor를 비우면 최신 메시지부터, 응답의 nextCursor를 넘기면 그보다 이전 메시지를 조회합니다. messages는 오래된 순으로 정렬됩니다.")
    @GetMapping
    public CommonResponse<ChatMessagePageResponse> getMessages(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId,
            @Parameter(description = "이 메시지 ID보다 이전 메시지를 조회 (첫 조회 시 생략)") @RequestParam(required = false) Long cursor,
            @Parameter(description = "조회 개수 (1~100)", example = "30") @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size
    ) {
        ChatMessagePageResponse response = chatMessageGetUseCase.getMessages(memberId, appointmentId, cursor, size);
        return CommonResponse.success(ResponseMessage.CHAT_MESSAGES_GET_SUCCESS, response);
    }

    @Operation(summary = "메시지 읽음 처리", description = "마지막으로 읽은 메시지 ID를 저장합니다(앞으로만 갱신). 갱신되면 /sub/appointments/{appointmentId}/read로 읽음 이벤트를 전송하며, 변경이 없으면 data는 null입니다. 채팅 중에는 STOMP /pub/appointments/{appointmentId}/read로도 보낼 수 있습니다.")
    @PutMapping("/read")
    public CommonResponse<ChatReadResponse> readMessages(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId,
            @RequestBody @Valid ChatReadRequest request
    ) {
        ChatReadResponse response = chatReadUseCase.markAsRead(memberId, appointmentId, request)
                .map(readResponse -> {
                    messagingTemplate.convertAndSend("/sub/appointments/" + appointmentId + "/read", readResponse);
                    return readResponse;
                })
                .orElse(null);
        return CommonResponse.success(ResponseMessage.CHAT_READ_SUCCESS, response);
    }
}
