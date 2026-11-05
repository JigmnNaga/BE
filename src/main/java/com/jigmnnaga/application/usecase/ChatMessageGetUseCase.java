package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.response.ChatMessagePageResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.ChatMessageMapper;
import com.jigmnnaga.domain.entity.ChatMessage;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.domain.service.ChatMessageGetService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ChatMessageGetUseCase {

    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final ChatMessageGetService chatMessageGetService;
    private final ChatMessageMapper chatMessageMapper;

    @Transactional(readOnly = true)
    public ChatMessagePageResponse getMessages(Long memberId, Long appointmentId, Long cursor, int size) {
        appointmentParticipantGetService.findAcceptedByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));

        List<ChatMessage> newestFirst = chatMessageGetService.findAllBeforeCursor(appointmentId, cursor, size + 1);
        return chatMessageMapper.toPageResponse(newestFirst, size);
    }
}
