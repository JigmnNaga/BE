-- V16: 회원 프로필 이미지(S3 키)와 기본 위치 추가

ALTER TABLE member
    ADD COLUMN profile_image_key VARCHAR(255) NULL,
    ADD COLUMN default_location_name VARCHAR(255) NULL,
    ADD COLUMN default_location_address VARCHAR(255) NULL,
    ADD COLUMN default_latitude DECIMAL(10, 7) NULL,
    ADD COLUMN default_longitude DECIMAL(10, 7) NULL;
