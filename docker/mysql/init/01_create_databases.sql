-- ============================================================
-- Docker MySQL 초기화 스크립트
-- 4개 서비스 DB를 단일 MySQL 컨테이너에 분리 생성
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

-- 각 서비스 전용 유저 생성 및 권한 부여
CREATE USER IF NOT EXISTS 'his_user'@'%' IDENTIFIED BY 'his_password';

GRANT ALL PRIVILEGES ON hospitalmsa_iam.*      TO 'his_user'@'%';
GRANT ALL PRIVILEGES ON hospitalmsa_admin.*    TO 'his_user'@'%';
GRANT ALL PRIVILEGES ON hospitalmsa_clinical.* TO 'his_user'@'%';
GRANT ALL PRIVILEGES ON hospitalmsa_support.*  TO 'his_user'@'%';

FLUSH PRIVILEGES;
