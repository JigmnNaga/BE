-- V24: 지각 예상 알림 이력 관리 컬럼 추가 (참여자 단위)
-- ETA 반복 계산 시 중복 발송 방지 및 서버 재시작 후에도 이력 유지

ALTER TABLE appointment_participant
    ADD COLUMN late_notif_sent_at DATETIME(6) NULL COMMENT '지각 예상 알림 발송 시간';
