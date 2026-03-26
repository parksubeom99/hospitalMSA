-- =============================================================================
-- Admin-Master Service Initial Schema (V1)
-- Target DB: hospitalmsa_admin
-- 기준: 현재 adminMasterService 엔티티 + 기존 운영 보조 테이블(admin_payment_idempotency)
-- =============================================================================

CREATE TABLE IF NOT EXISTS admission_exec (
                                              admission_exec_id BIGINT NOT NULL AUTO_INCREMENT,
                                              final_order_id BIGINT NOT NULL,
                                              ward VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(80) NOT NULL,
    admitted_at DATETIME NULL,
    discharged_at DATETIME NULL,
    updated_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (admission_exec_id),
    UNIQUE KEY uk_admission_exec_idempotency_key (idempotency_key),
    KEY idx_adm_final_order (final_order_id),
    KEY idx_adm_idem (idempotency_key)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS surgery_exec (
                                            surgery_exec_id BIGINT NOT NULL AUTO_INCREMENT,
                                            final_order_id BIGINT NOT NULL,
                                            surgery_name VARCHAR(128) NOT NULL,
    room VARCHAR(64) NULL,
    surgeon VARCHAR(80) NULL,
    status VARCHAR(32) NOT NULL,
    scheduled_at DATETIME NULL,
    completed_at DATETIME NULL,
    updated_at DATETIME NULL,
    idempotency_key VARCHAR(80) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (surgery_exec_id),
    UNIQUE KEY uk_surgery_exec_idempotency_key (idempotency_key),
    KEY idx_surg_final_order (final_order_id),
    KEY idx_surg_idem (idempotency_key)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_appointment (
                                                 appointment_id BIGINT NOT NULL AUTO_INCREMENT,
                                                 patient_id BIGINT NULL,
                                                 patient_name VARCHAR(255) NULL,
    department_code VARCHAR(255) NULL,
    doctor_id VARCHAR(255) NULL,
    status VARCHAR(255) NULL,
    scheduled_at DATETIME NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (appointment_id),
    KEY idx_admin_appt_scheduled (scheduled_at),
    KEY idx_admin_appt_status (status),
    KEY idx_admin_appt_patient (patient_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_visit (
                                           visit_id BIGINT NOT NULL AUTO_INCREMENT,
                                           patient_id BIGINT NULL,
                                           patient_name VARCHAR(255) NULL,
    department_code VARCHAR(255) NULL,
    doctor_id VARCHAR(255) NULL,
    status VARCHAR(255) NULL,
    arrival_type VARCHAR(20) NULL,
    triage_level INT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    canceled_at DATETIME NULL,
    cancel_reason VARCHAR(255) NULL,
    closed_at DATETIME NULL,
    PRIMARY KEY (visit_id),
    KEY idx_admin_visit_status (status),
    KEY idx_admin_visit_patient (patient_id),
    KEY idx_admin_visit_arrival_type (arrival_type),
    KEY idx_admin_visit_triage_level (triage_level),
    KEY idx_admin_visit_status_created (status, created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_reservation (
                                                 reservation_id BIGINT NOT NULL AUTO_INCREMENT,
                                                 patient_id BIGINT NULL,
                                                 patient_name VARCHAR(255) NULL,
    department_code VARCHAR(255) NULL,
    doctor_id VARCHAR(255) NULL,
    scheduled_at DATETIME NULL,
    status VARCHAR(255) NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    canceled_at DATETIME NULL,
    cancel_reason VARCHAR(255) NULL,
    visit_id BIGINT NULL,
    PRIMARY KEY (reservation_id),
    KEY idx_admin_resv_status (status),
    KEY idx_admin_resv_scheduled (scheduled_at),
    KEY idx_admin_resv_patient (patient_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_queue_ticket (
                                                  ticket_id BIGINT NOT NULL AUTO_INCREMENT,
                                                  visit_id BIGINT NULL,
                                                  category VARCHAR(255) NULL,
    ticket_no VARCHAR(255) NULL,
    status VARCHAR(255) NULL,
    issued_at DATETIME NULL,
    called_at DATETIME NULL,
    done_at DATETIME NULL,
    last_event_id VARCHAR(255) NULL,
    PRIMARY KEY (ticket_id),
    KEY idx_admin_queue_status (status),
    KEY idx_admin_queue_visit (visit_id),
    KEY idx_admin_queue_category (category)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_invoice (
                                             invoice_id BIGINT NOT NULL AUTO_INCREMENT,
                                             visit_id BIGINT NULL,
                                             status VARCHAR(255) NULL,
    total_amount BIGINT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (invoice_id),
    KEY idx_admin_invoice_visit (visit_id),
    KEY idx_admin_invoice_status (status),
    KEY idx_admin_invoice_visit_status (visit_id, status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_invoice_item (
                                                  invoice_item_id BIGINT NOT NULL AUTO_INCREMENT,
                                                  invoice_id BIGINT NULL,
                                                  item_code VARCHAR(255) NULL,
    item_name VARCHAR(255) NULL,
    unit_price BIGINT NULL,
    qty INT NULL,
    line_total BIGINT NULL,
    PRIMARY KEY (invoice_item_id),
    KEY idx_admin_invoice_item_invoice (invoice_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_invoice_adjustment (
                                                        adjustment_id BIGINT NOT NULL AUTO_INCREMENT,
                                                        invoice_id BIGINT NULL,
                                                        type VARCHAR(255) NULL,
    old_amount INT NULL,
    new_amount INT NULL,
    reason VARCHAR(255) NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (adjustment_id),
    KEY idx_inv_adj_invoice (invoice_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_payment (
                                             payment_id BIGINT NOT NULL AUTO_INCREMENT,
                                             invoice_id BIGINT NULL,
                                             method VARCHAR(255) NULL,
    amount BIGINT NULL,
    status VARCHAR(255) NULL,
    idempotency_key VARCHAR(255) NULL,
    paid_at DATETIME NULL,
    PRIMARY KEY (payment_id),
    UNIQUE KEY uk_admin_payment_idempotency_key (idempotency_key),
    KEY idx_admin_payment_invoice (invoice_id),
    KEY idx_admin_payment_idem (idempotency_key),
    KEY idx_admin_payment_invoice_status (invoice_id, status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_receipt (
                                             receipt_id BIGINT NOT NULL AUTO_INCREMENT,
                                             payment_id BIGINT NULL,
                                             receipt_no VARCHAR(255) NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (receipt_id),
    KEY idx_admin_receipt_payment (payment_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_payment_idempotency (
                                                         idem_key VARCHAR(120) NOT NULL,
    invoice_id BIGINT NULL,
    payment_id BIGINT NULL,
    request_amount BIGINT NULL,
    request_method VARCHAR(30) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    PRIMARY KEY (idem_key),
    KEY idx_admin_pay_idem_invoice (invoice_id),
    KEY idx_admin_pay_idem_payment (payment_id),
    KEY idx_admin_pay_idem_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS code_set (
                                        code_set_id BIGINT NOT NULL AUTO_INCREMENT,
                                        code_set_key VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (code_set_id),
    UNIQUE KEY uk_code_set_code_set_key (code_set_key)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS code_item (
                                         code_item_id BIGINT NOT NULL AUTO_INCREMENT,
                                         code_set_id BIGINT NOT NULL,
                                         code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (code_item_id),
    KEY idx_code_item_code_set_id (code_set_id),
    CONSTRAINT fk_code_item_code_set
    FOREIGN KEY (code_set_id) REFERENCES code_set(code_set_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS department (
                                          department_id BIGINT NOT NULL AUTO_INCREMENT,
                                          code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (department_id),
    UNIQUE KEY uk_department_code (code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS master_exam_catalog (
                                                   exam_catalog_id BIGINT NOT NULL AUTO_INCREMENT,
                                                   item_code VARCHAR(50) NOT NULL,
    category VARCHAR(20) NOT NULL,
    display_name_kr VARCHAR(100) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (exam_catalog_id),
    UNIQUE KEY uk_master_exam_catalog_code (item_code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS patient (
                                       patient_id BIGINT NOT NULL,
                                       name VARCHAR(255) NOT NULL,
    gender VARCHAR(255) NOT NULL,
    rrn_masked VARCHAR(255) NULL,
    birth_date DATE NULL,
    phone VARCHAR(255) NULL,
    active TINYINT(1) NOT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (patient_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS patient_alert (
                                             patient_alert_id BIGINT NOT NULL AUTO_INCREMENT,
                                             patient_id BIGINT NOT NULL,
                                             type VARCHAR(255) NOT NULL,
    message VARCHAR(255) NOT NULL,
    active TINYINT(1) NOT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (patient_alert_id),
    KEY idx_patient_alert_patient (patient_id),
    KEY idx_patient_alert_active (active)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS doctor_schedule_template (
                                                        schedule_template_id BIGINT NOT NULL AUTO_INCREMENT,
                                                        staff_profile_id BIGINT NOT NULL,
                                                        day_of_week INT NOT NULL,
                                                        start_time TIME NOT NULL,
                                                        end_time TIME NOT NULL,
                                                        slot_minutes INT NOT NULL,
                                                        active TINYINT(1) NOT NULL DEFAULT 1,
    note VARCHAR(200) NULL,
    PRIMARY KEY (schedule_template_id),
    KEY idx_dst_staff_day (staff_profile_id, day_of_week)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS staff_profile (
                                             staff_profile_id BIGINT NOT NULL AUTO_INCREMENT,
                                             login_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    job_type VARCHAR(50) NOT NULL,
    department_id BIGINT NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(150) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (staff_profile_id),
    UNIQUE KEY uk_staff_profile_login_id (login_id),
    KEY idx_staff_profile_phone (phone),
    KEY idx_staff_profile_email (email)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS outbox_event (
                                            id BIGINT NOT NULL AUTO_INCREMENT,
                                            event_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(32) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    topic VARCHAR(128) NOT NULL,
    partition_key VARCHAR(128) NULL,
    payload LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    fail_count INT NOT NULL,
    last_error VARCHAR(4000) NULL,
    created_at DATETIME NOT NULL,
    published_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_event_id (event_id),
    KEY ix_outbox_topic_status_created (topic, status, created_at),
    KEY ix_outbox_event_id (event_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS consumer_processed_event (
                                                        id BIGINT NOT NULL AUTO_INCREMENT,
                                                        event_id VARCHAR(64) NOT NULL,
    processed_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_processed_event_id (event_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;