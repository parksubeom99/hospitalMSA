-- Support Service v3 Patch
-- 1) 결과 수정/아카이브(정책 기반) 컬럼 추가
-- 2) Outbox 테이블 추가 (Kafka publish + idempotency)

-- LAB
ALTER TABLE lab_result
  ADD COLUMN updated_at DATETIME NULL,
  ADD COLUMN updated_by VARCHAR(50) NULL,
  ADD COLUMN archived TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN archived_at DATETIME NULL,
  ADD COLUMN archived_by VARCHAR(50) NULL,
  ADD COLUMN archived_reason VARCHAR(255) NULL;

-- RAD
ALTER TABLE radiology_report
  ADD COLUMN updated_at DATETIME NULL,
  ADD COLUMN updated_by VARCHAR(50) NULL,
  ADD COLUMN archived TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN archived_at DATETIME NULL,
  ADD COLUMN archived_by VARCHAR(50) NULL,
  ADD COLUMN archived_reason VARCHAR(255) NULL;

-- PHARM
ALTER TABLE dispense
  ADD COLUMN updated_at DATETIME NULL,
  ADD COLUMN updated_by VARCHAR(50) NULL,
  ADD COLUMN archived TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN archived_at DATETIME NULL,
  ADD COLUMN archived_by VARCHAR(50) NULL,
  ADD COLUMN archived_reason VARCHAR(255) NULL;

-- Outbox
CREATE TABLE IF NOT EXISTS support_outbox_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id VARCHAR(36) NOT NULL,
  event_type VARCHAR(50) NOT NULL,
  aggregate_type VARCHAR(50) NOT NULL,
  aggregate_id VARCHAR(64) NOT NULL,
  payload TEXT NOT NULL,
  dedup_key VARCHAR(120) NOT NULL,
  status VARCHAR(20) NOT NULL,
  retry_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  published_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_support_outbox_dedup (dedup_key),
  KEY idx_support_outbox_status_id (status, id)
);
