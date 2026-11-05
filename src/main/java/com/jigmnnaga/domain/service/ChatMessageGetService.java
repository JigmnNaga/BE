package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.ChatMessage;
import com.jigmnnaga.domain.entity.ChatMessageType;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import com.jigmnnaga.domain.repository.ChatMessageRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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

    public Optional<Long> findLatestMessageId(Long appointmentId) {
        return chatMessageRepository.findLatestMessageId(appointmentId);
    }

    public Map<Long, Long> countUnreadTextMessagesByAppointment(Long memberId) {
        return chatMessageRepository.countUnreadMessagesByAppointment(
                        memberId, ParticipationStatus.ACCEPTED, ChatMessageType.TEXT).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }
}
