-- V6: 채팅 메시지 테이블 추가 (약속 삭제 시 메시지도 함께 삭제)

CREATE TABLE chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) DEFAULT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    type ENUM('SYSTEM','TEXT') NOT NULL,
    content VARCHAR(1000) NOT NULL,
    appointment_id BIGINT NOT NULL,
    sender_id BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_chat_message_appointment_id (appointment_id, id),
    CONSTRAINT fk_chat_message_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointment (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_message_sender
        FOREIGN KEY (sender_id) REFERENCES member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
