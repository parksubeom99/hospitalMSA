-- IAM baseline V3 secondary indexes

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'user_account' AND index_name = 'idx_user_account_active'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_user_account_active ON user_account (active)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'user_role' AND index_name = 'idx_user_role_user'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_user_role_user ON user_role (user_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'user_role' AND index_name = 'idx_user_role_role'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_user_role_role ON user_role (role_code)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'role_permission' AND index_name = 'idx_role_perm_role'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_role_perm_role ON role_permission (role_code)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'role_permission' AND index_name = 'idx_role_perm_perm'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_role_perm_perm ON role_permission (perm_code)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND index_name = 'idx_audit_created_at'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_audit_created_at ON audit_log (created_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND index_name = 'idx_audit_actor'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_audit_actor ON audit_log (actor_login_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND index_name = 'idx_audit_action'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_audit_action ON audit_log (action)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND index_name = 'idx_audit_service'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_audit_service ON audit_log (service_name)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND index_name = 'idx_audit_result'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_audit_result ON audit_log (result)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
