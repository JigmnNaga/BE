-- V21: 약속 자동 완료 처리를 위한 완료 시각 컬럼 추가
-- 확정된 약속 시간으로부터 24시간 경과 시 스케줄러가 completedAt을 기록하여 중복 처리를 방지

ALTER TABLE appointment
    ADD COLUMN completed_at DATETIME(6) DEFAULT NULL;

SET @now_kst = DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 9 HOUR);

-- 배포 직후 이미 24시간이 경과한 기존 약속이 한꺼번에 완료 처리되지 않도록 백필
UPDATE appointment
SET completed_at = @now_kst
WHERE canceled = 0
  AND completed_at IS NULL
  AND confirmed_date IS NOT NULL
  AND confirmed_time IS NOT NULL
  AND confirmed_location_id IS NOT NULL
  AND detail_place_name IS NOT NULL
  AND TIMESTAMP(confirmed_date, confirmed_time) <= DATE_ADD(@now_kst, INTERVAL -24 HOUR);
