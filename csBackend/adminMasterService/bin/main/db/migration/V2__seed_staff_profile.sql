-- =============================================================================
-- adminMasterService Seed Data (v2) - staff_profile 초기 데이터
-- INSERT IGNORE: 이미 있으면 건너뜀 (멱등성 보장)
-- =============================================================================

INSERT IGNORE INTO staff_profile
  (staff_profile_id, login_id, name, job_type, department_id, phone, email, active)
VALUES
  (1, 'lee.doctor',  '이순신',   'DOCTOR', 1,    '010-1111-1111', 'lee@hospital.local',  true),
  (2, 'kim.doctor',  '김시민',   'DOCTOR', 2,    '010-2222-2222', 'kim@hospital.local',  true),
  (3, 'park.doctor', '박혁거세', 'DOCTOR', 3,    '010-3333-3333', 'park@hospital.local', true),
  (4, 'adm1',        '원무1',    'ADMIN',  NULL, '010-4444-4444', 'adm1@hospital.local', true),
  (5, 'adm2',        '원무2',    'ADMIN',  NULL, '010-5555-5555', 'adm2@hospital.local', true);

-- 중복 등록된 6,7,8번 정리 (이전 작업 중 실수로 생긴 데이터)
DELETE FROM staff_profile WHERE staff_profile_id >= 6;

-- AUTO_INCREMENT를 6으로 고정 (다음 신규 등록은 6번부터)
ALTER TABLE staff_profile AUTO_INCREMENT = 6;
