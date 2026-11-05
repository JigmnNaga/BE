-- V4: 후보 장소에 지하철 노선 정보 추가 (예: "2호선, 신분당선")

ALTER TABLE appointment_candidate_location
    ADD COLUMN line_names VARCHAR(255) DEFAULT NULL;
