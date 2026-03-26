-- MasterService 확장: code_set.active 컬럼 추가
-- DB: hospitalmsa_master

ALTER TABLE code_set
    ADD COLUMN IF NOT EXISTS active BIT NOT NULL DEFAULT b'1';
