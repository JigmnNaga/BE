package com.jigmnnaga.application.mapper;

import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.global.client.s3.S3FileClient;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileImageUrlResolver {

    // 비공개 버킷이라 업로드한 이미지는 응답 시점에 임시 조회 URL로 내려준다.
    private static final Duration DOWNLOAD_URL_EXPIRATION = Duration.ofHours(1);

    private final S3FileClient s3FileClient;

    public String resolve(Member member) {
        if (member == null) {
            return null;
        }
        if (member.getProfileImageKey() != null) {
            return s3FileClient.presignDownloadUrl(member.getProfileImageKey(), DOWNLOAD_URL_EXPIRATION);
        }
        return member.getProfileImageUrl();
    }
}
