-- =============================================================================
-- Clinical Service V2 — Oracle Edition
-- Phase 2: Visit clinical status tracking
-- [REMOVED] backtick, ENGINE, CHARSET, COLLATE
-- [MODIFIED] DATETIME(6) → TIMESTAMP(6)
-- [MODIFIED] VARCHAR → VARCHAR2
-- =============================================================================

CREATE TABLE visit_clinical_status (
                                       visit_id        NUMBER        NOT NULL,
                                       clinical_status VARCHAR2(40)  DEFAULT 'SOAP_IN_PROGRESS' NOT NULL,
                                       updated_at      TIMESTAMP(6)  NOT NULL,
                                       CONSTRAINT pk_visit_clinical_status PRIMARY KEY (visit_id)
);
CREATE INDEX idx_vcs_status ON visit_clinical_status (clinical_status);

-- 상태 값 참고:
-- SOAP_IN_PROGRESS     : SOAP 작성 중
-- EXAM_REQUESTED       : 검사 요청 완료
-- EXAM_IN_PROGRESS     : Support worklist 생성 완료
-- FINAL_ORDER_READY    : DIAGNOSTIC_ORDER_COMPLETED 수신 → 최종오더 가능
-- FINAL_ORDER_CONFIRMED: 최종오더 확정
-- BILLABLE             : 후속 task 집계 완료
-- BILLED               : 청구 완료