-- =============================================================================
-- IAM baseline V3 secondary indexes — Oracle Edition
-- [MODIFIED] information_schema.statistics → Oracle user_indexes / user_ind_columns
-- [MODIFIED] SET @var + PREPARE stmt → Oracle PL/SQL 익명 블록
-- [ADDED] EXCEPTION WHEN OTHERS THEN NULL: 인덱스 중복 생성 방지 (ORA-01408)
-- =============================================================================

-- idx_user_account_active
BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_user_account_active ON user_account (active)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- idx_user_role_user
BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_user_role_user ON user_role (user_id)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- idx_user_role_role
BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_user_role_role ON user_role (role_code)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- idx_role_perm_role
BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_role_perm_role ON role_permission (role_code)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- idx_role_perm_perm
BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_role_perm_perm ON role_permission (perm_code)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- idx_audit_created_at
BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_audit_created_at ON audit_log (created_at)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- idx_audit_actor
BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_audit_actor ON audit_log (actor_login_id)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- idx_audit_action
BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_audit_action ON audit_log (action)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- idx_audit_service
BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_audit_service ON audit_log (service_name)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- idx_audit_result
BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_audit_result ON audit_log (result)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN RAISE; END IF;
END;
/