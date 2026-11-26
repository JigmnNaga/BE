ALTER TABLE member
    ADD COLUMN all_notifications_enabled  BIT(1) NOT NULL DEFAULT b'1',
    ADD COLUMN appointment_reminder_enabled BIT(1) NOT NULL DEFAULT b'1',
    ADD COLUMN departure_time_enabled      BIT(1) NOT NULL DEFAULT b'1',
    ADD COLUMN friend_arrival_enabled      BIT(1) NOT NULL DEFAULT b'1',
    ADD COLUMN marketing_enabled           BIT(1) NOT NULL DEFAULT b'0';
