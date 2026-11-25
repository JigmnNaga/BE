-- V26: 회원 프로필 색상 추가 (프로필 사진이 없을 때 표시할 배경색, 기존 회원은 기본 색상)

ALTER TABLE member
    ADD COLUMN profile_color VARCHAR(20) NOT NULL DEFAULT 'OLIVE';
