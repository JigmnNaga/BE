package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.ChatMessageSendRequest;
import com.jigmnnaga.application.dto.response.ChatMessageResponse;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.ChatMessageMapper;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.ChatMessage;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.domain.service.ChatMessageSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ChatMessageSendUseCase {

    private final AppointmentParticipantGetService appointmentParticipantGetService;
    private final ChatMessageSaveService chatMessageSaveService;
    private final ChatMessageMapper chatMessageMapper;

    @Transactional
    public ChatMessageResponse sendMessage(Long memberId, Long appointmentId, ChatMessageSendRequest request) {
        AppointmentParticipant participant = appointmentParticipantGetService
                .findAcceptedByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));
        Appointment appointment = participant.getAppointment();
        if (appointment.isCanceled()) {
            throw new BusinessException(ErrorCode.APPOINTMENT_CANCELED);
        }

        ChatMessage chatMessage = chatMessageSaveService.save(
                chatMessageMapper.toTextMessage(appointment, participant.getMember(), request.content().strip()));
        return chatMessageMapper.toResponse(chatMessage);
    }
}
