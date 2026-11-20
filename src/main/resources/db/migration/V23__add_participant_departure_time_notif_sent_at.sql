-- V23: 출발 시간 알림 이력 관리 컬럼 추가 (참여자 단위)
-- 출발 준비 알림(30분 전)과 출발 알림의 중복 발송 방지

ALTER TABLE appointment_participant
    ADD COLUMN departure_prepare_notif_sent_at    DATETIME(6) NULL COMMENT '출발 준비 알림(출발 30분 전) 발송 시간',
    ADD COLUMN departure_start_time_notif_sent_at DATETIME(6) NULL COMMENT '출발 알림(출발 권장 시각) 발송 시간';
