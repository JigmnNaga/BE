-- V5: 회원 탈퇴 시각 컬럼 추가 (탈퇴 회원은 개인정보를 익명화하고 행은 보존)

ALTER TABLE member
    ADD COLUMN withdrawn_at DATETIME(6) DEFAULT NULL;
