-- Admin-Master baseline V3 indexes (core)

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'admin_visit' AND index_name = 'idx_admin_visit_status_created_pre'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_admin_visit_status_created_pre ON admin_visit (status, created_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'admin_invoice' AND index_name = 'idx_admin_invoice_visit_status_pre'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_admin_invoice_visit_status_pre ON admin_invoice (visit_id, status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'admin_payment' AND index_name = 'idx_admin_payment_invoice_status_pre'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_admin_payment_invoice_status_pre ON admin_payment (invoice_id, status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
