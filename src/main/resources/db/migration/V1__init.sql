-- V1: 초기 데이터베이스 스키마
-- 기존 RDS는 baseline 처리하며, 신규 DB에서 실행됨

-- 1. 회원
CREATE TABLE member (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        created_at DATETIME(6) DEFAULT NULL,
                        updated_at DATETIME(6) DEFAULT NULL,
                        email VARCHAR(255) DEFAULT NULL,
                        kakao_id BIGINT NOT NULL,
                        nickname VARCHAR(255) NOT NULL,
                        profile_image_url VARCHAR(255) DEFAULT NULL,
                        role ENUM('USER') NOT NULL,
                        PRIMARY KEY (id),
                        UNIQUE KEY UKtqi1nx9ul3nx7guxpqycuvgue (kakao_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. 약속
CREATE TABLE appointment (
                             id BIGINT NOT NULL AUTO_INCREMENT,
                             created_at DATETIME(6) DEFAULT NULL,
                             updated_at DATETIME(6) DEFAULT NULL,
                             date_selection_type ENUM('MULTIPLE','RANGE','SINGLE') NOT NULL,
                             invite_code VARCHAR(255) NOT NULL,
                             response_deadline DATETIME(6) NOT NULL,
                             title VARCHAR(30) NOT NULL,
                             confirmed_date DATE DEFAULT NULL,
                             confirmed_time TIME DEFAULT NULL,
                             detail_place_address VARCHAR(255) DEFAULT NULL,
                             detail_place_latitude DOUBLE DEFAULT NULL,
                             detail_place_longitude DOUBLE DEFAULT NULL,
                             detail_place_name VARCHAR(255) DEFAULT NULL,
                             confirmed_location_id BIGINT DEFAULT NULL,
                             canceled BIT(1) NOT NULL DEFAULT b'0',
                             PRIMARY KEY (id),
                             UNIQUE KEY UKcv20vvcc96jvu878o06t96d27 (invite_code),
                             KEY FK19ap0wpwryikd3mlh038ai7nd (confirmed_location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. 약속 장소 후보
CREATE TABLE appointment_candidate_location (
                                                id BIGINT NOT NULL AUTO_INCREMENT,
                                                created_at DATETIME(6) DEFAULT NULL,
                                                updated_at DATETIME(6) DEFAULT NULL,
                                                address VARCHAR(255) DEFAULT NULL,
                                                latitude DOUBLE NOT NULL,
                                                longitude DOUBLE NOT NULL,
                                                place_name VARCHAR(255) NOT NULL,
                                                appointment_id BIGINT NOT NULL,
                                                PRIMARY KEY (id),
                                                KEY FKmtox6h42tq33vl453io4nmipo (appointment_id),
                                                CONSTRAINT FKmtox6h42tq33vl453io4nmipo
                                                    FOREIGN KEY (appointment_id) REFERENCES appointment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. 약속 날짜 후보
CREATE TABLE appointment_candidate_date (
                                            id BIGINT NOT NULL AUTO_INCREMENT,
                                            created_at DATETIME(6) DEFAULT NULL,
                                            updated_at DATETIME(6) DEFAULT NULL,
                                            candidate_date DATE NOT NULL,
                                            appointment_id BIGINT NOT NULL,
                                            PRIMARY KEY (id),
                                            KEY FKjn3rsfp171a8tc2k64gl5dpbk (appointment_id),
                                            CONSTRAINT FKjn3rsfp171a8tc2k64gl5dpbk
                                                FOREIGN KEY (appointment_id) REFERENCES appointment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 5. 약속 참여자
CREATE TABLE appointment_participant (
                                         id BIGINT NOT NULL AUTO_INCREMENT,
                                         created_at DATETIME(6) DEFAULT NULL,
                                         updated_at DATETIME(6) DEFAULT NULL,
                                         role ENUM('HOST','MEMBER') NOT NULL,
                                         status ENUM('ACCEPTED','DECLINED','PENDING') NOT NULL,
                                         appointment_id BIGINT NOT NULL,
                                         member_id BIGINT NOT NULL,
                                         preferred_location_id BIGINT DEFAULT NULL,
                                         attendance_status ENUM('ATTENDING','NOT_ATTENDING','PENDING')
        NOT NULL DEFAULT 'PENDING',
                                         PRIMARY KEY (id),
                                         UNIQUE KEY uk_appointment_participant_appointment_member
                                             (appointment_id, member_id),
                                         KEY FKdpx3ohauuc09uym3nt1pajtrs (member_id),
                                         KEY FKn9pekv22b1ke3jjt4rc14gu6x (preferred_location_id),
                                         CONSTRAINT FKdpx3ohauuc09uym3nt1pajtrs
                                             FOREIGN KEY (member_id) REFERENCES member (id),
                                         CONSTRAINT FKmgcpfxwr1f2ptx25yrg8nnp7b
                                             FOREIGN KEY (appointment_id) REFERENCES appointment (id),
                                         CONSTRAINT FKn9pekv22b1ke3jjt4rc14gu6x
                                             FOREIGN KEY (preferred_location_id)
                                                 REFERENCES appointment_candidate_location (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 6. 참여자 가능 시간
CREATE TABLE participant_availability (
                                          id BIGINT NOT NULL AUTO_INCREMENT,
                                          created_at DATETIME(6) DEFAULT NULL,
                                          updated_at DATETIME(6) DEFAULT NULL,
                                          available_date DATE NOT NULL,
                                          end_time TIME NOT NULL,
                                          start_time TIME NOT NULL,
                                          participant_id BIGINT NOT NULL,
                                          PRIMARY KEY (id),
                                          KEY FKa32j56m30q731v4x8c39q6p67 (participant_id),
                                          CONSTRAINT FKa32j56m30q731v4x8c39q6p67
                                              FOREIGN KEY (participant_id)
                                                  REFERENCES appointment_participant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 7. 순환 참조 외래키 추가
ALTER TABLE appointment
    ADD CONSTRAINT FK19ap0wpwryikd3mlh038ai7nd
        FOREIGN KEY (confirmed_location_id)
            REFERENCES appointment_candidate_location (id);
