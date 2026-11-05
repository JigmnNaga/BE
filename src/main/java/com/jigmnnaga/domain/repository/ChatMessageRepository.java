package com.jigmnnaga.domain.repository;

import com.jigmnnaga.domain.entity.ChatMessage;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("select m from ChatMessage m left join fetch m.sender "
            + "where m.appointment.id = :appointmentId and (:cursor is null or m.id < :cursor) "
            + "order by m.id desc")
    List<ChatMessage> findAllWithSenderByAppointmentIdBeforeCursor(
            @Param("appointmentId") Long appointmentId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
