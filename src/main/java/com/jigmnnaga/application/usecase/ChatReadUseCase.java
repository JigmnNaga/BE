package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.ChatReadRequest;
import com.jigmnnaga.application.dto.response.ChatReadResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.ChatMessageMapper;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.domain.service.ChatMessageGetService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ChatReadUseCase {

    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final ChatMessageGetService chatMessageGetService;
    private final ChatMessageMapper chatMessageMapper;

    @Transactional
    public Optional<ChatReadResponse> markAsRead(Long memberId, Long appointmentId, ChatReadRequest request) {
        AppointmentParticipant participant = appointmentParticipantGetService
                .findAcceptedByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));

        Optional<Long> latestMessageId = chatMessageGetService.findLatestMessageId(appointmentId);
        if (latestMessageId.isEmpty()) {
            return Optional.empty();
        }

        Long previousLastReadMessageId = participant.getLastReadMessageId();
        Long lastReadMessageId = Math.min(request.lastReadMessageId(), latestMessageId.get());
        if (!participant.readUpTo(lastReadMessageId)) {
            return Optional.empty();
        }
        return Optional.of(chatMessageMapper.toReadResponse(memberId, previousLastReadMessageId, lastReadMessageId));
    }
}
