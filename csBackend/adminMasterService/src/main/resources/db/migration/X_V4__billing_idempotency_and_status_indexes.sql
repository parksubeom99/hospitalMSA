
-- Step4 Billing hardening (Flyway) - idempotent hardened
CREATE TABLE IF NOT EXISTS admin_payment_idempotency (
    idem_key            VARCHAR(120) NOT NULL,
    invoice_id          BIGINT       NULL,
    payment_id          BIGINT       NULL,
    request_amount      BIGINT       NULL,
    request_method      VARCHAR(30)  NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at        DATETIME     NULL,
    PRIMARY KEY (idem_key)
);

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'admin_payment_idempotency' AND index_name = 'idx_admin_pay_idem_invoice'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_admin_pay_idem_invoice ON admin_payment_idempotency (invoice_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'admin_payment_idempotency' AND index_name = 'idx_admin_pay_idem_payment'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_admin_pay_idem_payment ON admin_payment_idempotency (payment_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'admin_payment_idempotency' AND index_name = 'idx_admin_pay_idem_status'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_admin_pay_idem_status ON admin_payment_idempotency (status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'admin_invoice' AND index_name = 'idx_admin_invoice_visit_status'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_admin_invoice_visit_status ON admin_invoice (visit_id, status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'admin_payment' AND index_name = 'idx_admin_payment_invoice_status'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_admin_payment_invoice_status ON admin_payment (invoice_id, status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'admin_visit' AND index_name = 'idx_admin_visit_status_created'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_admin_visit_status_created ON admin_visit (status, created_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
