package com.jigmnnaga.domain.repository;

import com.jigmnnaga.domain.entity.Notification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("select n from Notification n where n.member.id = :memberId "
            + "and (:cursor is null or n.id < :cursor) order by n.id desc")
    List<Notification> findAllByMemberIdBeforeCursor(
            @Param("memberId") Long memberId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    Optional<Notification> findByIdAndMemberId(Long id, Long memberId);

    long countByMemberIdAndReadAtIsNull(Long memberId);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.readAt = :readAt where n.member.id = :memberId and n.readAt is null")
    int markAllAsRead(@Param("memberId") Long memberId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("delete from Notification n where n.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}
