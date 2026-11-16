package com.jigmnnaga.domain.repository;

import com.jigmnnaga.domain.entity.AuthProvider;
import com.jigmnnaga.domain.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
