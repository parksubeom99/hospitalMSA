-- Admin-Master baseline V2 seed (minimal)
-- 신규 DB 부팅/조회 화면 기본 검증용 최소 샘플 데이터
INSERT INTO staff_profile (staff_id, staff_name, dept_code, role_code, active, created_at, updated_at)
VALUES ('ADMIN-001','관리자','ADMIN','ADMIN',1,NOW(),NOW())
ON DUPLICATE KEY UPDATE staff_name=VALUES(staff_name), dept_code=VALUES(dept_code), role_code=VALUES(role_code), active=VALUES(active), updated_at=NOW();
