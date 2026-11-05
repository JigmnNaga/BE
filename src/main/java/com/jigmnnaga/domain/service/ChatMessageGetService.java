package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.ChatMessage;
import com.jigmnnaga.domain.repository.ChatMessageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatMessageGetService {

    private final ChatMessageRepository chatMessageRepository;

    public List<ChatMessage> findAllBeforeCursor(Long appointmentId, Long cursor, int limit) {
        return chatMessageRepository.findAllWithSenderByAppointmentIdBeforeCursor(
                appointmentId, cursor, PageRequest.of(0, limit));
    }
}
