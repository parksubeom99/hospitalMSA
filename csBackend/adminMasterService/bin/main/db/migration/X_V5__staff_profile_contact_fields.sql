
-- Step6 Master Settings 보완 (Flyway) - idempotent hardened

SET @col_phone_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'staff_profile' AND column_name = 'phone'
);
SET @sql := IF(@col_phone_exists = 0, 'ALTER TABLE staff_profile ADD COLUMN phone VARCHAR(20) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_email_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'staff_profile' AND column_name = 'email'
);
SET @sql := IF(@col_email_exists = 0, 'ALTER TABLE staff_profile ADD COLUMN email VARCHAR(120) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'staff_profile' AND index_name = 'idx_staff_profile_phone'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_staff_profile_phone ON staff_profile (phone)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'staff_profile' AND index_name = 'idx_staff_profile_email'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_staff_profile_email ON staff_profile (email)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
