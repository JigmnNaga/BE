package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.DeviceToken;
import com.jigmnnaga.domain.repository.DeviceTokenRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceTokenGetService {

    private final DeviceTokenRepository deviceTokenRepository;

    public Optional<DeviceToken> findByToken(String token) {
        return deviceTokenRepository.findByToken(token);
    }

    public List<String> findAllTokensByMemberIds(Collection<Long> memberIds) {
        return deviceTokenRepository.findAllTokensByMemberIds(memberIds);
    }
}
