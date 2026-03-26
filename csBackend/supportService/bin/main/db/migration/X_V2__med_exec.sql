-- step5 MED 실행(약제) 작업 테이블
-- 실행 위치: MySQL CLI 또는 Workbench (hospitalmsa_support)

USE hospitalmsa_support;

CREATE TABLE IF NOT EXISTS med_exec (
  med_exec_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  final_order_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  note VARCHAR(400) NULL,
  idempotency_key VARCHAR(80) NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT uq_med_exec_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_med_exec_final_order ON med_exec(final_order_id);
CREATE INDEX idx_med_exec_created_at ON med_exec(created_at);
