-- V25: 회원별 위치 공유 설정 추가 (마이페이지 MYP-004, 기존 회원은 기본값 적용)

ALTER TABLE member
    ADD COLUMN location_sharing_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN location_sharing_scope VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANTS',
    ADD COLUMN location_sharing_start_minutes INT NOT NULL DEFAULT 30;
