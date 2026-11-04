-- V2: 약속방 내보내기 상태 추가

ALTER TABLE appointment_participant
    MODIFY COLUMN status
    ENUM('ACCEPTED','DECLINED','KICKED','PENDING') NOT NULL;
