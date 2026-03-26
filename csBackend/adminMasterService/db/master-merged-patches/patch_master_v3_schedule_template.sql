-- MasterService 확장: 의사 스케줄 템플릿(예약 확장 대비)
-- DB: hospitalmsa_master

CREATE TABLE IF NOT EXISTS doctor_schedule_template (
    schedule_template_id BIGINT NOT NULL AUTO_INCREMENT,
    staff_profile_id BIGINT NOT NULL,
    day_of_week INT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_minutes INT NOT NULL DEFAULT 10,
    active BIT NOT NULL DEFAULT b'1',
    note VARCHAR(200),
    PRIMARY KEY (schedule_template_id),
    KEY idx_dst_staff_day (staff_profile_id, day_of_week)
);
