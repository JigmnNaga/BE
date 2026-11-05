-- V12: 참여자별 약속방 알림 설정 추가 (기본 켜짐)

ALTER TABLE appointment_participant
    ADD COLUMN notification_enabled BIT(1) NOT NULL DEFAULT b'1';
