-- IAM v3 patch: login audit hardening + audit query/retention + RBAC permissions
-- 실행 위치: MySQL CLI 또는 Workbench
-- 대상 스키마: hospitalmsa_iam

-- 1) audit_log 컬럼 확장
ALTER TABLE audit_log
    ADD COLUMN service_name VARCHAR(30) NOT NULL DEFAULT 'IAM',
    ADD COLUMN result VARCHAR(10) NOT NULL DEFAULT 'SUCCESS',
    ADD COLUMN ip_address VARCHAR(64) NULL,
    ADD COLUMN user_agent VARCHAR(512) NULL,
    ADD COLUMN archived TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN archived_at DATETIME NULL;

-- action/actor_login_id가 NULL인 기존 데이터가 있다면 사전 정리 권장
UPDATE audit_log SET action='UNKNOWN' WHERE action IS NULL;
UPDATE audit_log SET actor_login_id='UNKNOWN' WHERE actor_login_id IS NULL;

-- 인덱스(없으면 생성)
CREATE INDEX idx_audit_created_at ON audit_log(created_at);
CREATE INDEX idx_audit_actor ON audit_log(actor_login_id);
CREATE INDEX idx_audit_action ON audit_log(action);
CREATE INDEX idx_audit_service ON audit_log(service_name);
CREATE INDEX idx_audit_result ON audit_log(result);

-- 2) Permission / Role-Permission 테이블
CREATE TABLE IF NOT EXISTS permission (
    perm_code VARCHAR(40) PRIMARY KEY,
    perm_name VARCHAR(255) NOT NULL,
    description VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(20) NOT NULL,
    perm_code VARCHAR(40) NOT NULL,
    CONSTRAINT uk_role_perm UNIQUE(role_code, perm_code),
    INDEX idx_role_perm_role(role_code),
    INDEX idx_role_perm_perm(perm_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) (선택) 최소 Permission seed (원하시면 수정/확장)
INSERT IGNORE INTO permission(perm_code, perm_name, description) VALUES
('VISIT_CREATE','Visit Create','접수 생성'),
('VISIT_CANCEL','Visit Cancel','접수 취소'),
('ORDER_START','Order Start','오더 착수/진행'),
('RESULT_WRITE','Result Write','검사/영상/조제 결과 기록'),
('AUDIT_VIEW','Audit View','감사로그 조회');

-- (선택) Role -> Permission 최소 매핑 예시
-- ADMIN(원무): 접수 생성/취소, 감사 조회
INSERT IGNORE INTO role_permission(role_code, perm_code) VALUES
('ADMIN','VISIT_CREATE'),
('ADMIN','VISIT_CANCEL'),
('ADMIN','AUDIT_VIEW');

-- DOC(의사): 오더 착수/결과 기록은 서비스 정책에 따라
INSERT IGNORE INTO role_permission(role_code, perm_code) VALUES
('DOC','ORDER_START'),
('DOC','RESULT_WRITE');

-- SYS: 감사 조회
INSERT IGNORE INTO role_permission(role_code, perm_code) VALUES
('SYS','AUDIT_VIEW');
