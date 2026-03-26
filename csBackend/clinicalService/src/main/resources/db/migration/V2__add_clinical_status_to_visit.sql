-- Phase 2: Visit clinical status tracking
-- visit_id 기준 clinical 진행 상태를 clinical 서비스 내에서 관리
CREATE TABLE IF NOT EXISTS `visit_clinical_status` (
    `visit_id`         BIGINT       NOT NULL,
    `clinical_status`  VARCHAR(40)  NOT NULL DEFAULT 'SOAP_IN_PROGRESS',
    `updated_at`       DATETIME(6)  NOT NULL,
    PRIMARY KEY (`visit_id`),
    KEY `idx_vcs_status` (`clinical_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 상태 값 참고:
-- SOAP_IN_PROGRESS    : SOAP 작성 중
-- EXAM_REQUESTED      : 검사 요청 완료 (→ DIAGNOSTIC_ORDER_SUBMITTED 발행 트리거)
-- EXAM_IN_PROGRESS    : Support worklist 생성 완료
-- FINAL_ORDER_READY   : DIAGNOSTIC_ORDER_COMPLETED 수신 → 최종오더 가능
-- FINAL_ORDER_CONFIRMED: 최종오더 확정 (→ FINAL_ORDER_FINALIZED 발행)
-- BILLABLE            : 후속 task 집계 완료 (→ BILLING_REQUESTED 발행)
-- BILLED              : 청구 완료
