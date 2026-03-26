-- =============================================================================
-- Clinical Service Initial Schema (V1)
-- Target DB: hospitalmsa_clinical
-- =============================================================================

CREATE TABLE IF NOT EXISTS `order_header` (
                                              `order_id` bigint NOT NULL AUTO_INCREMENT,
                                              `category` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `deleted` bit(1) NOT NULL,
    `deleted_at` datetime(6) DEFAULT NULL,
    `deleted_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `deleted_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `idempotency_key` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `primary_item_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `primary_item_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `updated_at` datetime(6) DEFAULT NULL,
    `visit_id` bigint NOT NULL,
    PRIMARY KEY (`order_id`),
    UNIQUE KEY `UKnpsqnqipdoh67cill60gjgdcp` (`idempotency_key`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `order_item` (
                                            `order_item_id` bigint NOT NULL AUTO_INCREMENT,
                                            `item_code` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `order_id` bigint NOT NULL,
    `quantity` int NOT NULL,
    PRIMARY KEY (`order_item_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `visit_soap` (
                                            `visit_id` bigint NOT NULL,
                                            `archived` bit(1) NOT NULL,
    `archived_at` datetime(6) DEFAULT NULL,
    `archived_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `archived_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `assessment` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at` datetime(6) NOT NULL,
    `objective` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `plan` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `subjective` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `updated_at` datetime(6) NOT NULL,
    `version_no` int NOT NULL,
    PRIMARY KEY (`visit_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `visit_soap_history` (
                                                    `history_id` bigint NOT NULL AUTO_INCREMENT,
                                                    `assessment` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `captured_at` datetime(6) NOT NULL,
    `captured_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `objective` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `plan` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `subjective` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `version_no` int NOT NULL,
    `visit_id` bigint NOT NULL,
    PRIMARY KEY (`history_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `encounter_note` (
                                                `note_id` bigint NOT NULL AUTO_INCREMENT,
                                                `archived` bit(1) NOT NULL,
    `archived_at` datetime(6) DEFAULT NULL,
    `archived_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `archived_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at` datetime(6) NOT NULL,
    `note` varchar(4000) COLLATE utf8mb4_unicode_ci NOT NULL,
    `updated_at` datetime(6) DEFAULT NULL,
    `updated_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `visit_id` bigint NOT NULL,
    PRIMARY KEY (`note_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `final_order` (
                                             `final_order_id` bigint NOT NULL AUTO_INCREMENT,
                                             `created_at` datetime(6) NOT NULL,
    `idempotency_key` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
    `note` varchar(400) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
    `type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `visit_id` bigint NOT NULL,
    PRIMARY KEY (`final_order_id`),
    UNIQUE KEY `idx_final_order_idem` (`idempotency_key`),
    KEY `idx_final_order_visit` (`visit_id`),
    KEY `idx_final_order_status` (`status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `outbox_event` (
                                              `id` bigint NOT NULL AUTO_INCREMENT,
                                              `event_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
    `aggregate_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
    `aggregate_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
    `event_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
    `topic` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
    `partition_key` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `payload` tinytext COLLATE utf8mb4_unicode_ci NOT NULL,
    `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
    `fail_count` int NOT NULL DEFAULT '0',
    `last_error` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `published_at` datetime DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_outbox_event_event_id` (`event_id`),
    KEY `idx_outbox_status_created` (`status`, `created_at`),
    KEY `idx_outbox_aggregate` (`aggregate_type`, `aggregate_id`),
    KEY `ix_outbox_topic_status_created` (`topic`, `status`, `created_at`),
    KEY `ix_outbox_event_id` (`event_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `consumer_processed_event` (
                                                          `id` bigint NOT NULL AUTO_INCREMENT,
                                                          `event_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
    `processed_at` datetime(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_processed_event_id` (`event_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;