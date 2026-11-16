-- V20: 참여자 도착 시각 추가 (제때 도착·지각 통계에 사용)

ALTER TABLE appointment_participant
    ADD COLUMN arrived_at DATETIME(6) NULL;
