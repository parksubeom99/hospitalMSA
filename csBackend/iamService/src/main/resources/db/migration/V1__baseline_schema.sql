-- IAM baseline V1 schema
CREATE TABLE IF NOT EXISTS role (
    role_code VARCHAR(20) NOT NULL,
    role_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (role_code)
);

CREATE TABLE IF NOT EXISTS permission (
    perm_code VARCHAR(40) NOT NULL,
    perm_name VARCHAR(255) NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (perm_code)
);

CREATE TABLE IF NOT EXISTS role_permission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role_code VARCHAR(20) NOT NULL,
    perm_code VARCHAR(40) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_perm UNIQUE (role_code, perm_code)
);

CREATE TABLE IF NOT EXISTS user_account (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    login_id VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    staff_id VARCHAR(255) NOT NULL,
    active TINYINT(1) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_user_account_login_id UNIQUE (login_id)
);

CREATE TABLE IF NOT EXISTS user_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_code VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_role_user_role UNIQUE (user_id, role_code)
);

CREATE TABLE IF NOT EXISTS audit_log (
    event_id VARCHAR(40) NOT NULL,
    actor_login_id VARCHAR(255) NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    result VARCHAR(255) NOT NULL,
    target_type VARCHAR(255) NULL,
    target_id VARCHAR(255) NULL,
    patient_id BIGINT NULL,
    ip_address VARCHAR(255) NULL,
    user_agent VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL,
    archived TINYINT(1) NOT NULL DEFAULT 0,
    archived_at DATETIME NULL,
    detail_json TEXT NULL,
    PRIMARY KEY (event_id)
);
