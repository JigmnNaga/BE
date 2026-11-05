package com.jigmnnaga.domain.repository;

import com.jigmnnaga.domain.entity.ChatMessage;
import com.jigmnnaga.domain.entity.ChatMessageType;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import java.util.List;
import java.util.Optional;
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

    @Query("select max(m.id) from ChatMessage m where m.appointment.id = :appointmentId")
    Optional<Long> findLatestMessageId(@Param("appointmentId") Long appointmentId);

    @Query("select m.appointment.id, count(m) from ChatMessage m, AppointmentParticipant p "
            + "where p.appointment = m.appointment and p.member.id = :memberId and p.status = :status "
            + "and m.type = :type and m.id > coalesce(p.lastReadMessageId, 0) "
            + "and (m.sender is null or m.sender.id <> :memberId) "
            + "group by m.appointment.id")
    List<Object[]> countUnreadMessagesByAppointment(@Param("memberId") Long memberId,
                                                    @Param("status") ParticipationStatus status,
                                                    @Param("type") ChatMessageType type);
}
