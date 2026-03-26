-- =============================================================================
-- Clinical Service DB Patch (v2)
-- Adds: SOAP version/history/archive, Encounter update/archive, Order soft-delete
-- Target DB: hospitalmsa_clinical (or your clinical schema)
-- =============================================================================

/* 1) ORDER soft delete ------------------------------------------------------- */
ALTER TABLE order_header
    ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 AFTER updated_at,
    ADD COLUMN deleted_at DATETIME NULL AFTER deleted,
    ADD COLUMN deleted_by VARCHAR(100) NULL AFTER deleted_at,
    ADD COLUMN deleted_reason VARCHAR(255) NULL AFTER deleted_by;

CREATE INDEX idx_order_header_deleted_status_cat
    ON order_header (deleted, status, category);

/* 2) SOAP version + archive -------------------------------------------------- */
ALTER TABLE visit_soap
    ADD COLUMN version_no INT NOT NULL DEFAULT 1 AFTER plan,
    ADD COLUMN created_at DATETIME NULL AFTER version_no,
    ADD COLUMN archived TINYINT(1) NOT NULL DEFAULT 0 AFTER updated_at,
    ADD COLUMN archived_at DATETIME NULL AFTER archived,
    ADD COLUMN archived_by VARCHAR(100) NULL AFTER archived_at,
    ADD COLUMN archived_reason VARCHAR(255) NULL AFTER archived_by;

-- 기존 데이터 보정: created_at이 비어있으면 updated_at으로 채움
UPDATE visit_soap
SET created_at = COALESCE(created_at, updated_at, NOW())
WHERE created_at IS NULL;

ALTER TABLE visit_soap
    MODIFY COLUMN created_at DATETIME NOT NULL;

/* 2-1) SOAP history table (append-only) ------------------------------------- */
CREATE TABLE IF NOT EXISTS visit_soap_history (
                                                  history_id BIGINT NOT NULL AUTO_INCREMENT,
                                                  visit_id BIGINT NOT NULL,
                                                  version_no INT NOT NULL,
                                                  subjective VARCHAR(4000) NULL,
    objective VARCHAR(4000) NULL,
    assessment VARCHAR(4000) NULL,
    plan VARCHAR(4000) NULL,
    captured_at DATETIME NOT NULL,
    captured_by VARCHAR(100) NULL,
    PRIMARY KEY (history_id),
    UNIQUE KEY uk_soap_hist_visit_ver (visit_id, version_no),
    KEY idx_soap_hist_visit (visit_id)
    );

/* 3) Encounter update + archive --------------------------------------------- */
ALTER TABLE encounter_note
    ADD COLUMN updated_at DATETIME NULL AFTER created_at,
    ADD COLUMN updated_by VARCHAR(100) NULL AFTER updated_at,
    ADD COLUMN archived TINYINT(1) NOT NULL DEFAULT 0 AFTER updated_by,
    ADD COLUMN archived_at DATETIME NULL AFTER archived,
    ADD COLUMN archived_by VARCHAR(100) NULL AFTER archived_at,
    ADD COLUMN archived_reason VARCHAR(255) NULL AFTER archived_by;

CREATE INDEX idx_encounter_visit_archived
    ON encounter_note (visit_id, archived);
