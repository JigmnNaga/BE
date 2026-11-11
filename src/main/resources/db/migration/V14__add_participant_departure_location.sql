-- V14: 참여자별 출발 위치 정보 추가

ALTER TABLE appointment_participant
    ADD COLUMN departure_location_name VARCHAR(255) DEFAULT NULL,
    ADD COLUMN departure_latitude DECIMAL(10, 7) DEFAULT NULL,
    ADD COLUMN departure_longitude DECIMAL(10, 7) DEFAULT NULL;