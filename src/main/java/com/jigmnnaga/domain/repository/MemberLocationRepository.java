package com.jigmnnaga.domain.repository;

import com.jigmnnaga.domain.entity.MemberLocation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberLocationRepository extends JpaRepository<MemberLocation, Long> {

    List<MemberLocation> findAllByMemberIdOrderByIdAsc(Long memberId);

    Optional<MemberLocation> findByIdAndMemberId(Long id, Long memberId);

    long countByMemberId(Long memberId);

    @Modifying
    @Query("delete from MemberLocation l where l.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}
