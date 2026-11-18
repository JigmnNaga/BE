-- V22: 출발 정보 입력 요청 알림 이력 관리 컬럼 추가 (참여자 단위)
-- 최초/정기(AM·PM)/임박 각 단계별 중복 발송 방지

ALTER TABLE appointment_participant
    ADD COLUMN departure_notif_initial_sent_at    DATETIME(6) NULL,
    ADD COLUMN departure_notif_periodic_am_sent_at DATETIME(6) NULL,
    ADD COLUMN departure_notif_periodic_pm_sent_at DATETIME(6) NULL,
    ADD COLUMN departure_notif_upcoming_24h_sent_at DATETIME(6) NULL,
    ADD COLUMN departure_notif_upcoming_12h_sent_at DATETIME(6) NULL,
    ADD COLUMN departure_notif_upcoming_6h_sent_at  DATETIME(6) NULL,
    ADD COLUMN departure_notif_upcoming_3h_sent_at  DATETIME(6) NULL,
    ADD COLUMN departure_notif_upcoming_1h_sent_at  DATETIME(6) NULL;
