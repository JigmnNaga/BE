package com.jigmnnaga.application.usecase;

import com.jigmnnaga.application.dto.request.DefaultLocationRequest;
import com.jigmnnaga.application.dto.request.ProfileImageUploadUrlRequest;
import com.jigmnnaga.application.dto.request.ProfileUpdateRequest;
import com.jigmnnaga.application.dto.response.MyInfoResponse;
import com.jigmnnaga.application.dto.response.ProfileImageUploadUrlResponse;
import com.jigmnnaga.application.event.ProfileImageDiscardedEvent;
import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.application.mapper.MemberMapper;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.service.MemberGetService;
import com.jigmnnaga.global.client.s3.S3FileClient;
import com.jigmnnaga.global.client.s3.S3FileClient.PresignedUpload;
import com.jigmnnaga.global.util.TimeZones;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MemberProfileUseCase {

    private static final String PROFILE_IMAGE_PREFIX = "profiles/";
    private static final Duration UPLOAD_URL_EXPIRATION = Duration.ofMinutes(10);
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final MemberGetService memberGetService;
    private final MemberMapper memberMapper;
    private final S3FileClient s3FileClient;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ProfileImageUploadUrlResponse issueUploadUrl(Long memberId, ProfileImageUploadUrlRequest request) {
        String extension = IMAGE_EXTENSIONS.get(request.contentType());
        if (extension == null) {
            throw new BusinessException(ErrorCode.INVALID_PROFILE_IMAGE_TYPE);
        }

        String key = profileImagePrefix(memberId) + UUID.randomUUID() + "." + extension;
        PresignedUpload upload = s3FileClient.presignUpload(
                key, request.contentType(), request.contentLength(), UPLOAD_URL_EXPIRATION);

        return ProfileImageUploadUrlResponse.builder()
                .uploadUrl(upload.url())
                .profileImageKey(key)
                .expiresAt(LocalDateTime.ofInstant(upload.expiresAt(), TimeZones.SEOUL))
                .build();
    }

    @Transactional
    public MyInfoResponse updateProfile(Long memberId, ProfileUpdateRequest request) {
        if (request.nickname() == null && request.profileImageKey() == null) {
            throw new BusinessException(ErrorCode.EMPTY_PROFILE_UPDATE);
        }
        Member member = getMember(memberId);

        if (request.nickname() != null) {
            member.updateNickname(request.nickname().strip());
        }
        if (request.profileImageKey() != null && !request.profileImageKey().equals(member.getProfileImageKey())) {
            validateUploadedImage(memberId, request.profileImageKey());
            discardProfileImage(member);
            member.changeProfileImage(request.profileImageKey());
        }
        return memberMapper.toMyInfoResponse(member);
    }

    @Transactional
    public void deleteProfileImage(Long memberId) {
        Member member = getMember(memberId);
        discardProfileImage(member);
        member.removeProfileImage();
    }

    @Transactional
    public MyInfoResponse updateDefaultLocation(Long memberId, DefaultLocationRequest request) {
        Member member = getMember(memberId);
        member.updateDefaultLocation(request.name().strip(), request.address(), request.latitude(), request.longitude());
        return memberMapper.toMyInfoResponse(member);
    }

    @Transactional
    public void deleteDefaultLocation(Long memberId) {
        getMember(memberId).clearDefaultLocation();
    }

    private void validateUploadedImage(Long memberId, String key) {
        if (!key.startsWith(profileImagePrefix(memberId)) || key.contains("..")) {
            throw new BusinessException(ErrorCode.INVALID_PROFILE_IMAGE_KEY);
        }
        if (!s3FileClient.exists(key)) {
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_NOT_UPLOADED);
        }
    }

    private void discardProfileImage(Member member) {
        if (member.getProfileImageKey() != null) {
            applicationEventPublisher.publishEvent(new ProfileImageDiscardedEvent(member.getProfileImageKey()));
        }
    }

    private String profileImagePrefix(Long memberId) {
        return PROFILE_IMAGE_PREFIX + memberId + "/";
    }

    private Member getMember(Long memberId) {
        return memberGetService.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
