-- =============================================================================
-- adminMasterService Seed Data (v3) - admin_visit 초기 데이터
-- 결제 완료 후 visit 상태를 COMPLETED로 변경할 때 admin_visit에 해당 row가 있어야 함
-- INSERT IGNORE: 이미 있으면 건너뜀 (멱등성 보장)
-- =============================================================================

INSERT IGNORE INTO admin_visit
  (visit_id, patient_id, patient_name, department_code, doctor_id, status, created_at, updated_at)
VALUES
  (11001, 2001, '박서준', 'INTERNAL', 'lee.doctor',  'WAITING',      NOW(), NOW()),
  (11002, 2001, '박민재', 'INTERNAL', 'lee.doctor',  'IN_TREATMENT', NOW(), NOW()),
  (11003, 2002, '정수현', 'INTERNAL', 'kim.doctor',  'WAITING',      NOW(), NOW()),
  (11004, 2003, '이지은', 'INTERNAL', 'park.doctor', 'WAITING',      NOW(), NOW()),
  (11005, 2002, '박서준', 'INTERNAL', 'kim.doctor',  'WAITING',      NOW(), NOW()),
  (11006, 2002, '최수진', 'INTERNAL', 'park.doctor', 'WAITING',      NOW(), NOW());

ALTER TABLE admin_visit AUTO_INCREMENT = 12000;
