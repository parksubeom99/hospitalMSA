-- =============================================================================
-- Clinical Service V3 — Oracle Edition
-- Phase 2: visit_clinical_status에 patient_name 컬럼 추가
--
-- 근거:
--   Kafka VISIT_REGISTERED 이벤트에 patientName이 포함되어 있으나, 기존에는
--   Consumer에서 로그에만 사용되고 영속화되지 않음. 진료화면 드롭다운에서
--   환자명을 표시하려면 visit_clinical_status에 함께 저장 필요.
-- =============================================================================

ALTER TABLE visit_clinical_status ADD (patient_name VARCHAR2(100));
