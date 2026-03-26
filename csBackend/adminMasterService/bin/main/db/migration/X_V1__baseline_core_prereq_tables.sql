-- Admin-Master baseline V1 (core prerequisite tables for clean startup + later migrations V4/V5)
-- 목적: 최소 핵심 테이블 뼈대를 먼저 확보하여 V4/V5가 신규 DB에서도 적용 가능하게 함
-- 주의: 전체 도메인 완전 baseline이 아니라 우선 운영 핵심 + V4/V5 의존 테이블 중심

CREATE TABLE IF NOT EXISTS admin_visit (
    visit_id BIGINT NOT NULL AUTO_INCREMENT,
    status VARCHAR(30) NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (visit_id)
);

CREATE TABLE IF NOT EXISTS admin_invoice (
    invoice_id BIGINT NOT NULL AUTO_INCREMENT,
    visit_id BIGINT NULL,
    status VARCHAR(30) NULL,
    total_amount BIGINT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (invoice_id)
);

CREATE TABLE IF NOT EXISTS admin_payment (
    payment_id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_id BIGINT NULL,
    status VARCHAR(30) NULL,
    amount BIGINT NULL,
    method VARCHAR(30) NULL,
    paid_at DATETIME NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (payment_id)
);

CREATE TABLE IF NOT EXISTS staff_profile (
    staff_id VARCHAR(40) NOT NULL,
    staff_name VARCHAR(100) NULL,
    dept_code VARCHAR(40) NULL,
    role_code VARCHAR(20) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (staff_id)
);
