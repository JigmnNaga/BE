package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.ChatMessageSendRequest;
import com.jigmnnaga.application.dto.response.ChatMessageResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.usecase.ChatMessageSendUseCase;
import com.jigmnnaga.global.response.CommonResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private static final String APPOINTMENT_TOPIC_PREFIX = "/sub/appointments/";

    private final ChatMessageSendUseCase chatMessageSendUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/appointments/{appointmentId}/messages")
    public void sendMessage(
            @DestinationVariable Long appointmentId,
            @Payload @Valid ChatMessageSendRequest request,
            Principal principal
    ) {
        ChatMessageResponse response = chatMessageSendUseCase.sendMessage(
                Long.valueOf(principal.getName()), appointmentId, request);
        messagingTemplate.convertAndSend(APPOINTMENT_TOPIC_PREFIX + appointmentId, response);
    }

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/errors")
    public CommonResponse<Void> handleBusinessException(BusinessException e) {
        return CommonResponse.fail(e.getErrorCode().getMessage());
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser("/queue/errors")
    public CommonResponse<Void> handleInvalidMessage(MethodArgumentNotValidException e) {
        return CommonResponse.fail(ErrorCode.INVALID_CHAT_MESSAGE.getMessage());
    }
}
