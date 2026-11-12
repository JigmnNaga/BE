package com.jigmnnaga.global.exception;

import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    private final JsonMapper jsonMapper;

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        BusinessException businessException = findBusinessException(ex);
        if (businessException == null) {
            return super.handleClientMessageProcessingError(clientMessage, ex);
        }

        ErrorCode errorCode = businessException.getErrorCode();
        String errorMessage = errorCode.getMessage();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(errorMessage);
        accessor.setContentType(MimeTypeUtils.APPLICATION_JSON);
        accessor.setLeaveMutable(true);
        byte[] payload = jsonMapper.writeValueAsBytes(CommonResponse.fail(errorCode));
        return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
    }

    private BusinessException findBusinessException(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof BusinessException businessException) {
                return businessException;
            }
            cause = cause.getCause();
        }
        return null;
    }
}
