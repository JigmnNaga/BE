package com.jigmnnaga.domain.service;

import com.jigmnnaga.domain.entity.MemberLocation;
import com.jigmnnaga.domain.repository.MemberLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberLocationSaveService {

    private final MemberLocationRepository memberLocationRepository;

    public MemberLocation save(MemberLocation memberLocation) {
        return memberLocationRepository.save(memberLocation);
    }
}
