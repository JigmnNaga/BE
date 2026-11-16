package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.AuthProvider;
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

    public Optional<Member> findByProviderAndProviderId(AuthProvider provider, String providerId) {
        return memberRepository.findByProviderAndProviderId(provider, providerId);
    }

    public Optional<Member> findById(Long memberId) {
        return memberRepository.findById(memberId);
    }

    public List<Member> findAllByIds(List<Long> memberIds) {
        return memberRepository.findAllById(memberIds);
    }
}
