-- [NEW] Clinical Outbox table for Kafka publisher
CREATE TABLE IF NOT EXISTS outbox_event (
                                            id BIGINT NOT NULL AUTO_INCREMENT,
                                            event_id VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    topic VARCHAR(200) NOT NULL,
    partition_key VARCHAR(200) NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    fail_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_event_event_id (event_id),
    KEY idx_outbox_status_created (status, created_at),
    KEY idx_outbox_aggregate (aggregate_type, aggregate_id)
    );