-- V17: 기본 위치를 회원당 여러 개 저장하도록 member_location 테이블로 분리

CREATE TABLE member_location (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) DEFAULT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    member_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255) DEFAULT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_member_location_member_id (member_id),
    CONSTRAINT fk_member_location_member
        FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO member_location (created_at, updated_at, member_id, name, address, latitude, longitude)
SELECT NOW(6), NOW(6), id, default_location_name, default_location_address, default_latitude, default_longitude
FROM member
WHERE default_latitude IS NOT NULL
  AND default_longitude IS NOT NULL
  AND default_location_name IS NOT NULL;

ALTER TABLE member
    DROP COLUMN default_location_name,
    DROP COLUMN default_location_address,
    DROP COLUMN default_latitude,
    DROP COLUMN default_longitude;
