-- =============================================================================
-- Oracle XE 초기화 스크립트
-- [ADDED] 4개 서비스 전용 Oracle 사용자(스키마) 생성
-- 실행 대상: XEPDB1 (Pluggable DB — XE 21c 기본 PDB)
-- 실행 방법: /opt/oracle/scripts/setup/ 에 마운트 시 DB 최초 생성 후 자동 실행
-- [NOTE] Oracle XE는 MySQL의 DATABASE 개념 대신 USER(스키마)가 DB 역할을 담당
-- [NOTE] RESOURCE 롤에 CREATE TABLE, SEQUENCE, PROCEDURE, TRIGGER, INDEX, TYPE 포함
--        별도 GRANT CREATE TRIGGER 등 불필요 — ORA-00990 유발하므로 제거
-- =============================================================================

-- setup 스크립트는 CDB$ROOT(sysdba) 컨텍스트로 실행됨
-- local user는 반드시 PDB(XEPDB1) 컨텍스트에서 생성해야 함
ALTER SESSION SET CONTAINER = XEPDB1;

-- ── IAM 서비스 ──────────────────────────────────────────────────────────────
BEGIN
    EXECUTE IMMEDIATE 'CREATE USER his_iam IDENTIFIED BY his_password DEFAULT TABLESPACE USERS QUOTA UNLIMITED ON USERS';
EXCEPTION
    WHEN OTHERS THEN IF SQLCODE != -01920 THEN RAISE; END IF;
END;
/
-- [MODIFIED] RESOURCE 롤에 CREATE TABLE/SEQUENCE/PROCEDURE/TRIGGER/INDEX 포함
-- [REMOVED] 불필요한 두 번째 GRANT — ORA-00990 유발 (CREATE TRIGGER 단독 GRANT 불가)
GRANT CONNECT, RESOURCE, CREATE VIEW, CREATE SESSION TO his_iam;
GRANT UNLIMITED TABLESPACE TO his_iam;

-- ── Admin-Master 서비스 ────────────────────────────────────────────────────
BEGIN
    EXECUTE IMMEDIATE 'CREATE USER his_admin IDENTIFIED BY his_password DEFAULT TABLESPACE USERS QUOTA UNLIMITED ON USERS';
EXCEPTION
    WHEN OTHERS THEN IF SQLCODE != -01920 THEN RAISE; END IF;
END;
/
GRANT CONNECT, RESOURCE, CREATE VIEW, CREATE SESSION TO his_admin;
GRANT UNLIMITED TABLESPACE TO his_admin;

-- ── Clinical 서비스 ────────────────────────────────────────────────────────
BEGIN
    EXECUTE IMMEDIATE 'CREATE USER his_clinical IDENTIFIED BY his_password DEFAULT TABLESPACE USERS QUOTA UNLIMITED ON USERS';
EXCEPTION
    WHEN OTHERS THEN IF SQLCODE != -01920 THEN RAISE; END IF;
END;
/
GRANT CONNECT, RESOURCE, CREATE VIEW, CREATE SESSION TO his_clinical;
GRANT UNLIMITED TABLESPACE TO his_clinical;

-- ── Support 서비스 ─────────────────────────────────────────────────────────
BEGIN
    EXECUTE IMMEDIATE 'CREATE USER his_support IDENTIFIED BY his_password DEFAULT TABLESPACE USERS QUOTA UNLIMITED ON USERS';
EXCEPTION
    WHEN OTHERS THEN IF SQLCODE != -01920 THEN RAISE; END IF;
END;
/
GRANT CONNECT, RESOURCE, CREATE VIEW, CREATE SESSION TO his_support;
GRANT UNLIMITED TABLESPACE TO his_support;

EXIT;