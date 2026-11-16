-- V18: 참여자별 위치 공유 여부 추가 (사용자가 공유를 끄면 false)

ALTER TABLE appointment_participant
    ADD COLUMN location_sharing_enabled BOOLEAN NOT NULL DEFAULT TRUE;
