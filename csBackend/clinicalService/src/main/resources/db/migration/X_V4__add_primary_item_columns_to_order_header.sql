ALTER TABLE order_header
    ADD COLUMN primary_item_code VARCHAR(100) NULL AFTER idempotency_key,
    ADD COLUMN primary_item_name VARCHAR(255) NULL AFTER primary_item_code;