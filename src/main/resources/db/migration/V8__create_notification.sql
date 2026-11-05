-- V8: 알림 테이블 추가
-- type은 알림 유형 추가 시 마이그레이션이 필요 없도록 VARCHAR로 저장
-- appointment_id는 약속 삭제 후에도 알림이 남도록 외래 키 없이 저장

CREATE TABLE notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) DEFAULT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    member_id BIGINT NOT NULL,
    appointment_id BIGINT DEFAULT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    body VARCHAR(255) NOT NULL,
    read_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_notification_member_id (member_id, id),
    CONSTRAINT fk_notification_member
        FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
