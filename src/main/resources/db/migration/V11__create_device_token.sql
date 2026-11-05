-- V11: 푸시 알림(FCM) 기기 토큰 테이블 추가

CREATE TABLE device_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) DEFAULT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    member_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    platform VARCHAR(20) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_token_token (token),
    KEY idx_device_token_member_id (member_id),
    CONSTRAINT fk_device_token_member
        FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
