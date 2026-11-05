-- V7: 참여자별 마지막으로 읽은 채팅 메시지 ID 추가
-- 기존 참여자는 현재 최신 메시지까지 읽은 것으로 처리 (도입 시점에 과거 메시지가 모두 안 읽음으로 표시되지 않도록)

ALTER TABLE appointment_participant
    ADD COLUMN last_read_message_id BIGINT DEFAULT NULL;

UPDATE appointment_participant p
SET p.last_read_message_id = (
    SELECT MAX(m.id) FROM chat_message m WHERE m.appointment_id = p.appointment_id
);
