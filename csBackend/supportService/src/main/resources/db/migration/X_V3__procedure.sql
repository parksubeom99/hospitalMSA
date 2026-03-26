-- v5 PROC(시술/내시경) 결과 리포트(Order 기반)
-- 실행 위치: MySQL CLI 또는 Workbench (hospitalmsa_support)

USE hospitalmsa_support;

CREATE TABLE IF NOT EXISTS procedure_report (
  procedure_report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  report_text TEXT NULL,
  status VARCHAR(32) NOT NULL,
  idempotency_key VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT uq_procedure_report_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_procedure_report_order_id ON procedure_report(order_id);
CREATE INDEX idx_procedure_report_created_at ON procedure_report(created_at);
