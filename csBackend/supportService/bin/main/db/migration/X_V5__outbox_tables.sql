CREATE TABLE IF NOT EXISTS outbox_event (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100),
    aggregate_id VARCHAR(100),
    partition_key VARCHAR(100),
    topic VARCHAR(200),
    payload TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    fail_count INT DEFAULT 0,
    last_error VARCHAR(500),
    created_at DATETIME NOT NULL,
    published_at DATETIME,
    CONSTRAINT uk_outbox_event_id UNIQUE (event_id)
    );

CREATE TABLE IF NOT EXISTS support_outbox_event (
                                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100),
    aggregate_id VARCHAR(100),
    dedup_key VARCHAR(100),
    topic VARCHAR(200),
    payload TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    retry_count INT DEFAULT 0,
    created_at DATETIME NOT NULL,
    published_at DATETIME,
    CONSTRAINT uk_support_outbox_event_id UNIQUE (event_id)
    );