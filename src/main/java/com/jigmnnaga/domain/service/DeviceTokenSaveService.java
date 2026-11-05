package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.DeviceToken;
import com.jigmnnaga.domain.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceTokenSaveService {

    private final DeviceTokenRepository deviceTokenRepository;

    public void save(DeviceToken deviceToken) {
        deviceTokenRepository.save(deviceToken);
    }
}
