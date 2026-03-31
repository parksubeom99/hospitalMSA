-- =============================================================================
-- IAM baseline V2 seed — Oracle Edition
-- [MODIFIED] ON DUPLICATE KEY UPDATE → MERGE INTO (Oracle 미지원)
-- [MODIFIED] INSERT + SELECT FROM dual → Oracle 표준 문법
-- =============================================================================

-- ── roles ──────────────────────────────────────────────────────────────────
MERGE INTO role t
USING (
    SELECT 'SYS'   AS role_code, 'System'        AS role_name FROM dual UNION ALL
    SELECT 'ADMIN',              'Administrator'              FROM dual UNION ALL
    SELECT 'DOC',                'Doctor'                     FROM dual UNION ALL
    SELECT 'NUR',                'Nurse'                      FROM dual UNION ALL
    SELECT 'LAB',                'Laboratory'                 FROM dual UNION ALL
    SELECT 'RAD',                'Radiology'                  FROM dual UNION ALL
    SELECT 'PHARM',              'Pharmacy'                   FROM dual UNION ALL
    SELECT 'PROC',               'Procedure'                  FROM dual
) s ON (t.role_code = s.role_code)
WHEN MATCHED THEN
    UPDATE SET t.role_name = s.role_name
WHEN NOT MATCHED THEN
    INSERT (role_code, role_name) VALUES (s.role_code, s.role_name);

-- ── permissions ────────────────────────────────────────────────────────────
MERGE INTO permission t
USING (
    SELECT 'AUTH_LOGIN'   AS perm_code, '로그인'   AS perm_name, 'Authentication login' AS description FROM dual UNION ALL
    SELECT 'AUTH_REFRESH',              '토큰갱신',               'Refresh token'                      FROM dual UNION ALL
    SELECT 'AUDIT_WRITE',               '감사기록',               'Write audit logs'                   FROM dual
) s ON (t.perm_code = s.perm_code)
WHEN MATCHED THEN
    UPDATE SET t.perm_name = s.perm_name, t.description = s.description
WHEN NOT MATCHED THEN
    INSERT (perm_code, perm_name, description) VALUES (s.perm_code, s.perm_name, s.description);

-- ── role_permission ────────────────────────────────────────────────────────
MERGE INTO role_permission t
USING (
    SELECT 'SYS'   AS role_code, 'AUTH_LOGIN'   AS perm_code FROM dual UNION ALL
    SELECT 'SYS',                'AUTH_REFRESH'               FROM dual UNION ALL
    SELECT 'SYS',                'AUDIT_WRITE'                FROM dual UNION ALL
    SELECT 'ADMIN',              'AUTH_LOGIN'                 FROM dual UNION ALL
    SELECT 'ADMIN',              'AUTH_REFRESH'               FROM dual UNION ALL
    SELECT 'ADMIN',              'AUDIT_WRITE'                FROM dual UNION ALL
    SELECT 'DOC',                'AUTH_LOGIN'                 FROM dual UNION ALL
    SELECT 'NUR',                'AUTH_LOGIN'                 FROM dual UNION ALL
    SELECT 'LAB',                'AUTH_LOGIN'                 FROM dual UNION ALL
    SELECT 'RAD',                'AUTH_LOGIN'                 FROM dual UNION ALL
    SELECT 'PHARM',              'AUTH_LOGIN'                 FROM dual UNION ALL
    SELECT 'PROC',               'AUTH_LOGIN'                 FROM dual
) s ON (t.role_code = s.role_code AND t.perm_code = s.perm_code)
WHEN NOT MATCHED THEN
    INSERT (role_code, perm_code) VALUES (s.role_code, s.perm_code);

-- ── user_account ───────────────────────────────────────────────────────────
-- BCrypt hash values (known test seeds)
-- admin123 / doctor123 / nurse123 / lab123 / rad123 / pharm123 / proc123 / system
MERGE INTO user_account t
USING (
    SELECT 'system' AS login_id, '$2b$10$BoHJSr/voZw6hw2HwR4ILOES9cmA5usWEjJXWfnYrROuJSG49W0s6' AS password_hash, 'SYS-001'   AS staff_id, 1 AS active FROM dual UNION ALL
    SELECT 'admin',              '$2b$10$DqllEy7YOBly2Qhxj0PI1.bnH59292qbhVNzdc1pZuCenNlybCaoO',               'ADMIN-001',              1             FROM dual UNION ALL
    SELECT 'doctor',             '$2b$10$OLG/JnywJGkCX/M6tjAOiOwOuXnLAjLuyzKQSduHrcdBCuVIS0teW',               'DOC-001',                1             FROM dual UNION ALL
    SELECT 'nurse',              '$2b$10$pArrWLeHnHk36nGvthFcYeVey.PvpefbLJBjEwva7XhpJKZrgyvwi',               'NUR-001',                1             FROM dual UNION ALL
    SELECT 'lab',                '$2b$10$WNvCdOO.gsXm3odIkVS9qeTCCOi0REMtcbN0h9J9ZTwZDpqIMJAwS',               'LAB-001',                1             FROM dual UNION ALL
    SELECT 'rad',                '$2b$10$GGBsfQikzwBOsFOzs7kDGuHLi65dfvsSRQjNiTI7OwemVshD4FUUm',               'RAD-001',                1             FROM dual UNION ALL
    SELECT 'pharm',              '$2b$10$JhoHWw3AWYvAhMR3f94GDeDRP35I5pRjypiDiGYqytrmr4rxzbu.m',               'PHARM-001',              1             FROM dual UNION ALL
    SELECT 'proc',               '$2b$10$hP9cP5z1wviT0qVtWf.3FuxNGJv3GxW4TTS1fQ/4EHGgVmy6OeK8m',               'PROC-001',              1             FROM dual
) s ON (t.login_id = s.login_id)
WHEN MATCHED THEN
    UPDATE SET t.password_hash = s.password_hash, t.staff_id = s.staff_id, t.active = s.active
WHEN NOT MATCHED THEN
    INSERT (login_id, password_hash, staff_id, active)
    VALUES (s.login_id, s.password_hash, s.staff_id, s.active);

-- ── user_role ──────────────────────────────────────────────────────────────
MERGE INTO user_role t
USING (
    SELECT ua.user_id, x.role_code
    FROM (
        SELECT 'system' AS login_id, 'SYS'   AS role_code FROM dual UNION ALL
        SELECT 'admin',              'ADMIN'               FROM dual UNION ALL
        SELECT 'doctor',             'DOC'                 FROM dual UNION ALL
        SELECT 'nurse',              'NUR'                 FROM dual UNION ALL
        SELECT 'lab',                'LAB'                 FROM dual UNION ALL
        SELECT 'rad',                'RAD'                 FROM dual UNION ALL
        SELECT 'pharm',              'PHARM'               FROM dual UNION ALL
        SELECT 'proc',               'PROC'                FROM dual
    ) x
    JOIN user_account ua ON ua.login_id = x.login_id
) s ON (t.user_id = s.user_id AND t.role_code = s.role_code)
WHEN NOT MATCHED THEN
    INSERT (user_id, role_code) VALUES (s.user_id, s.role_code);
