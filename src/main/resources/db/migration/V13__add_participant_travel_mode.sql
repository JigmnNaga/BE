-- V13: 참여자별 약속 당일 이동수단 선택 정보 추가 (BUS, SUBWAY, TAXI)

ALTER TABLE appointment_participant
    ADD COLUMN travel_mode VARCHAR(20) DEFAULT NULL,
    ADD COLUMN travel_duration_minutes INT DEFAULT NULL,
    ADD COLUMN travel_mode_selected_at DATETIME(6) DEFAULT NULL;
