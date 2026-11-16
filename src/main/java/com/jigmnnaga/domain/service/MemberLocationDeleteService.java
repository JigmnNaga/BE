package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.MemberLocation;
import com.jigmnnaga.domain.repository.MemberLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberLocationDeleteService {

    private final MemberLocationRepository memberLocationRepository;

    public void delete(MemberLocation memberLocation) {
        memberLocationRepository.delete(memberLocation);
    }

    public void deleteAllByMemberId(Long memberId) {
        memberLocationRepository.deleteAllByMemberId(memberId);
    }
}
