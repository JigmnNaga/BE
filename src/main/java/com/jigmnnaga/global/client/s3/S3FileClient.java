package com.jigmnnaga.global.client.s3;

import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Slf4j
@Component
public class S3FileClient {

    private static final int NOT_FOUND_STATUS = 404;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3FileClient(S3Client s3Client, S3Presigner s3Presigner, @Value("${aws.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    public PresignedUpload presignUpload(String key, String contentType, long contentLength, Duration expiration) {
        try {
            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(builder -> builder
                    .signatureDuration(expiration)
                    .putObjectRequest(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .contentLength(contentLength)
                            .build()));
            return new PresignedUpload(presigned.url().toString(), presigned.expiration());
        } catch (SdkException e) {
            log.error("S3 업로드 URL 발급 실패: key={}", key, e);
            throw new BusinessException(ErrorCode.FILE_STORAGE_FAILED);
        }
    }

    public String presignDownloadUrl(String key, Duration expiration) {
        try {
            return s3Presigner.presignGetObject(builder -> builder
                            .signatureDuration(expiration)
                            .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build()))
                    .url()
                    .toString();
        } catch (SdkException e) {
            log.warn("S3 조회 URL 발급 실패: key={}", key, e);
            return null;
        }
    }

    public boolean exists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == NOT_FOUND_STATUS) {
                return false;
            }
            log.error("S3 파일 확인 실패: key={}", key, e);
            throw new BusinessException(ErrorCode.FILE_STORAGE_FAILED);
        } catch (SdkException e) {
            log.error("S3 파일 확인 실패: key={}", key, e);
            throw new BusinessException(ErrorCode.FILE_STORAGE_FAILED);
        }
    }

    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            log.warn("S3 파일 삭제 실패: key={}", key, e);
        }
    }

    public record PresignedUpload(String url, Instant expiresAt) {
    }
}
