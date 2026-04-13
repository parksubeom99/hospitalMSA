-- ============================================================
-- RDS MySQL 초기화 스크립트
-- EC2에서 mysql 클라이언트로 1회 실행
-- ============================================================

CREATE DATABASE IF NOT EXISTS hospitalmsa_iam
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS hospitalmsa_admin
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS hospitalmsa_clinical
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS hospitalmsa_support
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

FLUSH PRIVILEGES;
