package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.MemberLocation;
import com.jigmnnaga.domain.repository.MemberLocationRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberLocationGetService {

    private final MemberLocationRepository memberLocationRepository;

    public List<MemberLocation> findAllByMemberId(Long memberId) {
        return memberLocationRepository.findAllByMemberIdOrderByIdAsc(memberId);
    }

    public Optional<MemberLocation> findByIdAndMemberId(Long locationId, Long memberId) {
        return memberLocationRepository.findByIdAndMemberId(locationId, memberId);
    }

    public long countByMemberId(Long memberId) {
        return memberLocationRepository.countByMemberId(memberId);
    }
}
