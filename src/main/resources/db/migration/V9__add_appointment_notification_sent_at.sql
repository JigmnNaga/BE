-- V9: 예약 알림 중복 발송 방지를 위한 발송 시각 컬럼 추가
-- 약속 시각은 한국 시간으로 저장되므로, DB 시간대와 무관하게 UTC 기준 +9시간으로 현재 한국 시각을 계산
-- 배포 직후 이미 시점이 지난 기존 약속의 알림이 한꺼번에 발송되지 않도록 발송 완료로 백필

ALTER TABLE appointment
    ADD COLUMN deadline_reminder_sent_at DATETIME(6) DEFAULT NULL,
    ADD COLUMN deadline_closed_notified_at DATETIME(6) DEFAULT NULL,
    ADD COLUMN pre_appointment_notified_at DATETIME(6) DEFAULT NULL;

SET @now_kst = DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 9 HOUR);

UPDATE appointment
SET deadline_reminder_sent_at = @now_kst
WHERE response_deadline <= DATE_ADD(@now_kst, INTERVAL 24 HOUR);

UPDATE appointment
SET deadline_closed_notified_at = @now_kst
WHERE response_deadline <= @now_kst;

UPDATE appointment
SET pre_appointment_notified_at = @now_kst
WHERE confirmed_date IS NOT NULL
  AND TIMESTAMP(confirmed_date, confirmed_time) <= DATE_ADD(@now_kst, INTERVAL 5 HOUR);
