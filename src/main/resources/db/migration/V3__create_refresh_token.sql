-- V3: 리프레시 토큰 저장 테이블 추가 (회원당 1개, 토큰 원문 대신 SHA-256 해시 저장)

CREATE TABLE refresh_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) DEFAULT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    token_hash VARCHAR(64) NOT NULL,
    member_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_member (member_id),
    CONSTRAINT fk_refresh_token_member
        FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
