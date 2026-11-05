package com.jigmnnaga.domain.repository;

import com.jigmnnaga.domain.entity.DeviceToken;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    @Query("select d.token from DeviceToken d where d.member.id in :memberIds")
    List<String> findAllTokensByMemberIds(@Param("memberIds") Collection<Long> memberIds);

    @Modifying
    @Query("delete from DeviceToken d where d.token = :token and d.member.id = :memberId")
    int deleteByTokenAndMemberId(@Param("token") String token, @Param("memberId") Long memberId);

    @Modifying
    @Query("delete from DeviceToken d where d.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query("delete from DeviceToken d where d.token in :tokens")
    void deleteAllByTokens(@Param("tokens") Collection<String> tokens);
}
