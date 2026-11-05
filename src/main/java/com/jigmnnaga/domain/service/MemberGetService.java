package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.repository.MemberRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberGetService {

    private final MemberRepository memberRepository;

    public Optional<Member> findByKakaoId(Long kakaoId) {
        return memberRepository.findByKakaoId(kakaoId);
    }

    public Optional<Member> findById(Long memberId) {
        return memberRepository.findById(memberId);
    }

    public List<Member> findAllByIds(List<Long> memberIds) {
        return memberRepository.findAllById(memberIds);
    }
}
