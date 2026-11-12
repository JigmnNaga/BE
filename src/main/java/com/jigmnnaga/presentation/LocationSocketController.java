package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.LocationUpdateRequest;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.usecase.LocationShareUseCase;
import com.jigmnnaga.global.response.CommonResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class LocationSocketController {

    private final LocationShareUseCase locationShareUseCase;

    @MessageMapping("/appointments/{appointmentId}/location")
    public void updateLocation(
            @DestinationVariable Long appointmentId,
            @Payload @Valid LocationUpdateRequest request,
            Principal principal
    ) {
        locationShareUseCase.updateLocation(Long.valueOf(principal.getName()), appointmentId, request);
    }

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/errors")
    public CommonResponse<Void> handleBusinessException(BusinessException e) {
        return CommonResponse.fail(e.getErrorCode());
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser("/queue/errors")
    public CommonResponse<Void> handleInvalidMessage(MethodArgumentNotValidException e) {
        return CommonResponse.fail(ErrorCode.INVALID_INPUT_VALUE);
    }
}
