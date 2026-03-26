-- ============================================================
-- X_V6__support_complete_schema.sql
-- Support 서비스 전체 테이블 완성본
-- 엔티티 클래스 기반으로 생성 — IF NOT EXISTS로 중복 안전
-- ============================================================

-- ── 1. Outbox 이벤트 테이블 (IntegrationOutboxKafkaPublisher) ──
CREATE TABLE IF NOT EXISTS outbox_event (
                                            id            BIGINT       NOT NULL AUTO_INCREMENT,
                                            event_id      VARCHAR(64)  NOT NULL,
    event_type    VARCHAR(64)  NOT NULL,
    aggregate_type VARCHAR(32) NOT NULL,
    aggregate_id  VARCHAR(64)  NOT NULL,
    topic         VARCHAR(128) NOT NULL,
    partition_key VARCHAR(128),
    payload       LONGTEXT     NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    fail_count    INT          NOT NULL DEFAULT 0,
    last_error    VARCHAR(4000),
    created_at    DATETIME     NOT NULL,
    published_at  DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uk_outbox_event_id UNIQUE (event_id),
    INDEX ix_outbox_topic_status_created (topic, status, created_at),
    INDEX ix_outbox_event_id (event_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 2. Support Outbox 이벤트 테이블 (OutboxPublisher) ──
CREATE TABLE IF NOT EXISTS support_outbox_event (
                                                    id             BIGINT      NOT NULL AUTO_INCREMENT,
                                                    event_id       VARCHAR(64) NOT NULL,
    event_type     VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(32) NOT NULL,
    aggregate_id   VARCHAR(64) NOT NULL,
    payload        LONGTEXT    NOT NULL,
    dedup_key      VARCHAR(128) NOT NULL,
    status         VARCHAR(16) NOT NULL DEFAULT 'NEW',
    retry_count    INT         NOT NULL DEFAULT 0,
    created_at     DATETIME    NOT NULL,
    published_at   DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uk_support_outbox_dedup UNIQUE (dedup_key),
    INDEX ix_support_outbox_status (status, id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 3. Kafka 중복 소비 방지 테이블 (ProcessedEvent) ──
CREATE TABLE IF NOT EXISTS consumer_processed_event (
                                                        id           BIGINT      NOT NULL AUTO_INCREMENT,
                                                        event_id     VARCHAR(64) NOT NULL,
    processed_at DATETIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_processed_event_id UNIQUE (event_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 4. 오더 상태 동기화 실패 재시도 테이블 ──
CREATE TABLE IF NOT EXISTS order_status_sync_failure (
                                                         id            BIGINT       NOT NULL AUTO_INCREMENT,
                                                         order_id      BIGINT       NOT NULL,
                                                         target_status VARCHAR(30)  NOT NULL,
    endpoint_url  VARCHAR(255) NOT NULL,
    error_message VARCHAR(2000),
    trace_id      VARCHAR(120),
    sync_status   VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count   INT          NOT NULL DEFAULT 0,
    next_retry_at DATETIME     NOT NULL,
    last_tried_at DATETIME,
    succeeded_at  DATETIME,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_ossf_status_next_retry (sync_status, next_retry_at),
    INDEX idx_ossf_order_status (order_id, target_status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 5. Worklist 태스크 테이블 ──
CREATE TABLE IF NOT EXISTS support_worklist_task (
                                                     worklist_task_id  BIGINT      NOT NULL AUTO_INCREMENT,
                                                     order_id          BIGINT      NOT NULL,
                                                     visit_id          BIGINT      NOT NULL,
                                                     category          VARCHAR(20) NOT NULL,
    primary_item_code VARCHAR(50),
    primary_item_name VARCHAR(100),
    status            VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at        DATETIME    NOT NULL,
    updated_at        DATETIME,
    PRIMARY KEY (worklist_task_id),
    CONSTRAINT uk_support_worklist_task_order UNIQUE (order_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 6. 주사 실행 테이블 ──
CREATE TABLE IF NOT EXISTS injection_exec (
                                              injection_exec_id BIGINT      NOT NULL AUTO_INCREMENT,
                                              final_order_id    BIGINT      NOT NULL,
                                              status            VARCHAR(32) NOT NULL,
    note              VARCHAR(400),
    idempotency_key   VARCHAR(80) NOT NULL,
    created_at        DATETIME    NOT NULL,
    PRIMARY KEY (injection_exec_id),
    CONSTRAINT uk_inj_idem UNIQUE (idempotency_key),
    INDEX idx_inj_final_order (final_order_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 7. 복약 실행 테이블 ──
CREATE TABLE IF NOT EXISTS med_exec (
                                        med_exec_id     BIGINT      NOT NULL AUTO_INCREMENT,
                                        final_order_id  BIGINT      NOT NULL,
                                        status          VARCHAR(32) NOT NULL,
    note            VARCHAR(400),
    idempotency_key VARCHAR(80) NOT NULL,
    created_at      DATETIME    NOT NULL,
    PRIMARY KEY (med_exec_id),
    CONSTRAINT uk_med_idem UNIQUE (idempotency_key),
    INDEX idx_med_final_order (final_order_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 8. 내시경 리포트 테이블 ──
CREATE TABLE IF NOT EXISTS endoscopy_report (
                                                endoscopy_report_id BIGINT        NOT NULL AUTO_INCREMENT,
                                                final_order_id      BIGINT        NOT NULL,
                                                status              VARCHAR(32)   NOT NULL,
    report_text         VARCHAR(2000) NOT NULL,
    idempotency_key     VARCHAR(80)   NOT NULL,
    created_at          DATETIME      NOT NULL,
    PRIMARY KEY (endoscopy_report_id),
    CONSTRAINT uk_endo_idem UNIQUE (idempotency_key),
    INDEX idx_endo_final_order (final_order_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 9. 검사(Lab) 결과 테이블 ──
CREATE TABLE IF NOT EXISTS lab_result (
                                          lab_result_id   BIGINT      NOT NULL AUTO_INCREMENT,
                                          order_id        BIGINT      NOT NULL,
                                          result_text     TEXT,
                                          status          VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    created_at      DATETIME    NOT NULL,
    updated_at      DATETIME,
    updated_by      VARCHAR(100),
    archived        TINYINT(1)  NOT NULL DEFAULT 0,
    archived_at     DATETIME,
    archived_by     VARCHAR(100),
    archived_reason VARCHAR(500),
    PRIMARY KEY (lab_result_id),
    CONSTRAINT uk_lab_idem UNIQUE (idempotency_key)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 10. 약국 조제 테이블 ──
CREATE TABLE IF NOT EXISTS dispense (
                                        dispense_id     BIGINT      NOT NULL AUTO_INCREMENT,
                                        order_id        BIGINT      NOT NULL,
                                        dispense_text   TEXT,
                                        status          VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    created_at      DATETIME    NOT NULL,
    updated_at      DATETIME,
    updated_by      VARCHAR(100),
    archived        TINYINT(1)  NOT NULL DEFAULT 0,
    archived_at     DATETIME,
    archived_by     VARCHAR(100),
    archived_reason VARCHAR(500),
    PRIMARY KEY (dispense_id),
    CONSTRAINT uk_dispense_idem UNIQUE (idempotency_key)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 11. 시술 리포트 테이블 ──
CREATE TABLE IF NOT EXISTS procedure_report (
                                                procedure_report_id BIGINT       NOT NULL AUTO_INCREMENT,
                                                order_id            BIGINT       NOT NULL,
                                                report_text         TEXT,
                                                status              VARCHAR(50)  NOT NULL,
    idempotency_key     VARCHAR(255) NOT NULL,
    created_at          DATETIME     NOT NULL,
    PRIMARY KEY (procedure_report_id),
    CONSTRAINT uk_proc_idem UNIQUE (idempotency_key)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 12. 영상(Radiology) 리포트 테이블 ──
CREATE TABLE IF NOT EXISTS radiology_report (
                                                radiology_report_id BIGINT       NOT NULL AUTO_INCREMENT,
                                                order_id            BIGINT       NOT NULL,
                                                report_text         TEXT,
                                                status              VARCHAR(50)  NOT NULL,
    idempotency_key     VARCHAR(255) NOT NULL,
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME,
    updated_by          VARCHAR(100),
    archived            TINYINT(1)   NOT NULL DEFAULT 0,
    archived_at         DATETIME,
    archived_by         VARCHAR(100),
    archived_reason     VARCHAR(500),
    PRIMARY KEY (radiology_report_id),
    CONSTRAINT uk_rad_idem UNIQUE (idempotency_key)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
