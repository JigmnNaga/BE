-- V19: 회원 식별을 카카오 전용(kakao_id)에서 소셜 로그인 제공자(provider) + 제공자 회원 ID(provider_id)로 변경
-- 기존 회원은 모두 카카오 회원이다. kakao_id를 삭제하면 그 컬럼만으로 된 유니크 인덱스도 함께 삭제된다.

ALTER TABLE member
    ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'KAKAO',
    ADD COLUMN provider_id VARCHAR(255) NULL;

UPDATE member SET provider_id = CAST(kakao_id AS CHAR);

ALTER TABLE member
    MODIFY COLUMN provider_id VARCHAR(255) NOT NULL,
    ALTER COLUMN provider DROP DEFAULT,
    DROP COLUMN kakao_id,
    ADD CONSTRAINT uk_member_provider UNIQUE (provider, provider_id);
