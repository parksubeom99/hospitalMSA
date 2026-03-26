-- IAM baseline V2 seed (roles/users/rbac)
INSERT INTO role(role_code, role_name) VALUES
('SYS','System'),
('ADMIN','Administrator'),
('DOC','Doctor'),
('NUR','Nurse'),
('LAB','Laboratory'),
('RAD','Radiology'),
('PHARM','Pharmacy'),
('PROC','Procedure')
ON DUPLICATE KEY UPDATE role_name=VALUES(role_name);

INSERT INTO permission(perm_code, perm_name, description) VALUES
('AUTH_LOGIN','로그인','Authentication login'),
('AUTH_REFRESH','토큰갱신','Refresh token'),
('AUDIT_WRITE','감사기록','Write audit logs')
ON DUPLICATE KEY UPDATE perm_name=VALUES(perm_name), description=VALUES(description);

INSERT INTO role_permission(role_code, perm_code)
SELECT * FROM (
    SELECT 'SYS' AS role_code, 'AUTH_LOGIN' AS perm_code UNION ALL
    SELECT 'SYS','AUTH_REFRESH' UNION ALL
    SELECT 'SYS','AUDIT_WRITE' UNION ALL
    SELECT 'ADMIN','AUTH_LOGIN' UNION ALL
    SELECT 'ADMIN','AUTH_REFRESH' UNION ALL
    SELECT 'ADMIN','AUDIT_WRITE' UNION ALL
    SELECT 'DOC','AUTH_LOGIN' UNION ALL
    SELECT 'NUR','AUTH_LOGIN' UNION ALL
    SELECT 'LAB','AUTH_LOGIN' UNION ALL
    SELECT 'RAD','AUTH_LOGIN' UNION ALL
    SELECT 'PHARM','AUTH_LOGIN' UNION ALL
    SELECT 'PROC','AUTH_LOGIN'
) t
ON DUPLICATE KEY UPDATE role_code=VALUES(role_code), perm_code=VALUES(perm_code);

-- BCrypt hash values (known test seeds)
-- admin123 / doctor123 / nurse123 / lab123 / rad123 / pharm123 / proc123 / system
INSERT INTO user_account(login_id, password_hash, staff_id, active) VALUES
('system', '$2b$10$BoHJSr/voZw6hw2HwR4ILOES9cmA5usWEjJXWfnYrROuJSG49W0s6', 'SYS-001', 1),
('admin',  '$2b$10$DqllEy7YOBly2Qhxj0PI1.bnH59292qbhVNzdc1pZuCenNlybCaoO', 'ADMIN-001', 1),
('doctor', '$2b$10$OLG/JnywJGkCX/M6tjAOiOwOuXnLAjLuyzKQSduHrcdBCuVIS0teW', 'DOC-001', 1),
('nurse',  '$2b$10$pArrWLeHnHk36nGvthFcYeVey.PvpefbLJBjEwva7XhpJKZrgyvwi', 'NUR-001', 1),
('lab',    '$2b$10$WNvCdOO.gsXm3odIkVS9qeTCCOi0REMtcbN0h9J9ZTwZDpqIMJAwS', 'LAB-001', 1),
('rad',    '$2b$10$GGBsfQikzwBOsFOzs7kDGuHLi65dfvsSRQjNiTI7OwemVshD4FUUm', 'RAD-001', 1),
('pharm',  '$2b$10$JhoHWw3AWYvAhMR3f94GDeDRP35I5pRjypiDiGYqytrmr4rxzbu.m', 'PHARM-001', 1),
('proc',   '$2b$10$hP9cP5z1wviT0qVtWf.3FuxNGJv3GxW4TTS1fQ/4EHGgVmy6OeK8m', 'PROC-001', 1)
ON DUPLICATE KEY UPDATE
password_hash=VALUES(password_hash), staff_id=VALUES(staff_id), active=VALUES(active);

INSERT INTO user_role(user_id, role_code)
SELECT ua.user_id, x.role_code
FROM (
    SELECT 'system' AS login_id, 'SYS' AS role_code UNION ALL
    SELECT 'admin','ADMIN' UNION ALL
    SELECT 'doctor','DOC' UNION ALL
    SELECT 'nurse','NUR' UNION ALL
    SELECT 'lab','LAB' UNION ALL
    SELECT 'rad','RAD' UNION ALL
    SELECT 'pharm','PHARM' UNION ALL
    SELECT 'proc','PROC'
) x
JOIN user_account ua ON ua.login_id = x.login_id
ON DUPLICATE KEY UPDATE role_code=VALUES(role_code);
