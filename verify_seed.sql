-- =============================================================================
-- hospitalMSA Demo Seed Verification SQL
-- Usage:
--   docker cp verify_seed.sql psb-oracle:/tmp/verify_seed.sql
--   docker exec psb-oracle sqlplus his_admin/his_password@//localhost:1521/XEPDB1 @/tmp/verify_seed.sql
-- =============================================================================

SET LINESIZE 200
SET PAGESIZE 50
COLUMN patient_name FORMAT A20
COLUMN name FORMAT A20
COLUMN status FORMAT A15
COLUMN phone FORMAT A20
COLUMN arrival_type FORMAT A12

PROMPT === [1] 테이블별 건수 ===
SELECT 'patient' AS tbl, COUNT(*) AS cnt FROM patient
UNION ALL SELECT 'admin_reservation', COUNT(*) FROM admin_reservation
UNION ALL SELECT 'admin_appointment', COUNT(*) FROM admin_appointment
UNION ALL SELECT 'admin_visit', COUNT(*) FROM admin_visit
UNION ALL SELECT 'admin_invoice', COUNT(*) FROM admin_invoice
UNION ALL SELECT 'admin_queue_ticket', COUNT(*) FROM admin_queue_ticket;

PROMPT === [2] patient ===
SELECT patient_id, name, gender, phone FROM patient ORDER BY patient_id;

PROMPT === [3] admin_reservation ===
SELECT reservation_id, patient_name, department_code, doctor_id, status, TO_CHAR(scheduled_at, 'YYYY-MM-DD HH24:MI') AS scheduled FROM admin_reservation ORDER BY reservation_id;

PROMPT === [4] admin_visit ===
SELECT visit_id, patient_name, department_code, doctor_id, status, arrival_type, TO_CHAR(created_at, 'YYYY-MM-DD') AS created FROM admin_visit ORDER BY visit_id;

PROMPT === [5] admin_appointment ===
SELECT appointment_id, patient_name, status, TO_CHAR(scheduled_at, 'YYYY-MM-DD HH24:MI') AS scheduled FROM admin_appointment ORDER BY appointment_id;

PROMPT === [6] admin_config ===
SELECT config_key, config_value FROM admin_config;

EXIT;
