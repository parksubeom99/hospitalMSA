package kr.co.seoulit.his.clinical.domain.visitstatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Phase 2: Visit clinical 상태머신 서비스
 *
 * 상태 전이 규칙:
 * SOAP_IN_PROGRESS → EXAM_REQUESTED  (SOAP 저장 완료 후 검사요청 존재 시)
 * EXAM_REQUESTED   → EXAM_IN_PROGRESS (DIAGNOSTIC_ORDER_SUBMITTED 발행 시)
 * EXAM_IN_PROGRESS → FINAL_ORDER_READY (DIAGNOSTIC_ORDER_COMPLETED 수신 시)
 * FINAL_ORDER_READY → FINAL_ORDER_CONFIRMED (최종오더 확정 시)
 * FINAL_ORDER_CONFIRMED → BILLABLE (후속 task 집계 완료 시)
 * BILLABLE → BILLED (BILLING_COMPLETED 수신 시)
 * BILLABLE → BILLING_FAILED (BILLING_FAILED 수신 시 — 담당자 수동 재처리 대기) [ADDED]
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitClinicalStatusService {

    private final VisitClinicalStatusRepository repo;

    @Transactional(readOnly = true)
    public String getStatus(Long visitId) {
        return repo.findById(visitId)
                .map(VisitClinicalStatus::getClinicalStatus)
                .orElse("SOAP_IN_PROGRESS");
    }

    // [ADDED] 진료화면 드롭다운용 전체 목록 조회
    // BILLED/BILLING_FAILED는 진료 완료 상태이므로 제외
    @Transactional(readOnly = true)
    public java.util.List<VisitClinicalStatus> getAllActiveStatuses() {
        return repo.findAll().stream()
                .filter(vcs -> !"BILLED".equals(vcs.getClinicalStatus())
                        && !"BILLING_FAILED".equals(vcs.getClinicalStatus()))
                .sorted(java.util.Comparator.comparingLong(VisitClinicalStatus::getVisitId).reversed())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 초기화 또는 현재 상태 반환 (idempotent)
     */
    @Transactional
    public VisitClinicalStatus initOrGet(Long visitId) {
        return repo.findById(visitId).orElseGet(() -> repo.save(
                VisitClinicalStatus.builder()
                        .visitId(visitId)
                        .clinicalStatus("SOAP_IN_PROGRESS")
                        .updatedAt(LocalDateTime.now())
                        .build()
        ));
    }

    /**
     * SOAP 저장 완료 마킹
     * → 검사요청이 이미 존재하면 EXAM_REQUESTED로 전환 (호출자가 판단)
     */
    @Transactional
    public void markSoapDone(Long visitId, boolean hasExamOrder) {
        VisitClinicalStatus vcs = initOrGet(visitId);
        String cur = vcs.getClinicalStatus();

        if ("SOAP_IN_PROGRESS".equals(cur)) {
            String next = hasExamOrder ? "EXAM_REQUESTED" : "SOAP_IN_PROGRESS";
            if (!next.equals(cur)) {
                vcs.setClinicalStatus(next);
                vcs.setUpdatedAt(LocalDateTime.now());
                log.info("[VisitClinicalStatus] visitId={} {} → {}", visitId, cur, next);
            }
        }
    }

    /**
     * 검사요청 저장 완료 마킹
     * → SOAP이 이미 완료 상태이면 EXAM_REQUESTED로 전환
     */
    @Transactional
    public boolean markExamRequested(Long visitId) {
        VisitClinicalStatus vcs = initOrGet(visitId);
        String cur = vcs.getClinicalStatus();

        // 이미 이후 단계이면 무시 (idempotent)
        if (isAfterOrEqual(cur, "EXAM_REQUESTED")) {
            return false;
        }

        vcs.setClinicalStatus("EXAM_REQUESTED");
        vcs.setUpdatedAt(LocalDateTime.now());
        log.info("[VisitClinicalStatus] visitId={} {} → EXAM_REQUESTED", visitId, cur);
        return true; // → 호출자가 DIAGNOSTIC_ORDER_SUBMITTED 발행
    }

    /**
     * DIAGNOSTIC_ORDER_SUBMITTED 발행 후 → EXAM_IN_PROGRESS
     */
    @Transactional
    public void markExamInProgress(Long visitId) {
        transition(visitId, "EXAM_REQUESTED", "EXAM_IN_PROGRESS");
    }

    /**
     * DIAGNOSTIC_ORDER_COMPLETED 수신 → FINAL_ORDER_READY
     */
    @Transactional
    public void markFinalOrderReady(Long visitId) {
        VisitClinicalStatus vcs = initOrGet(visitId);
        String cur = vcs.getClinicalStatus();
        if (isAfterOrEqual(cur, "FINAL_ORDER_READY")) return;
        vcs.setClinicalStatus("FINAL_ORDER_READY");
        vcs.setUpdatedAt(LocalDateTime.now());
        log.info("[VisitClinicalStatus] visitId={} {} → FINAL_ORDER_READY", visitId, cur);
    }

    /**
     * 최종오더 확정 → FINAL_ORDER_CONFIRMED
     */
    @Transactional
    public void markFinalOrderConfirmed(Long visitId) {
        transition(visitId, "FINAL_ORDER_READY", "FINAL_ORDER_CONFIRMED");
    }

    /**
     * 후속 task 집계 완료 → BILLABLE
     */
    @Transactional
    public void markBillable(Long visitId) {
        VisitClinicalStatus vcs = initOrGet(visitId);
        String cur = vcs.getClinicalStatus();
        if (isAfterOrEqual(cur, "BILLABLE")) return;
        vcs.setClinicalStatus("BILLABLE");
        vcs.setUpdatedAt(LocalDateTime.now());
        log.info("[VisitClinicalStatus] visitId={} {} → BILLABLE", visitId, cur);
    }

    /**
     * BILLING_COMPLETED 수신 → BILLED
     */
    @Transactional
    public void markBilled(Long visitId) {
        transition(visitId, "BILLABLE", "BILLED");
    }

    // [ADDED] BILLING_FAILED 수신 → BILLING_FAILED 상태 명시
    // 이유: 청구 실패 시 담당자가 상태를 인지하고 수동 재처리할 수 있도록 명시적 상태 전환
    //       자동 롤백(BILLABLE 복귀)보다 실제 HIS 운영 패턴에 부합 (담당자 재처리 워크플로우)
    @Transactional
    public void markBillingFailed(Long visitId) {
        VisitClinicalStatus vcs = initOrGet(visitId);
        String cur = vcs.getClinicalStatus();
        // BILLED 이후에 FAILED가 오는 경우는 무시 (이미 완료)
        if ("BILLED".equalsIgnoreCase(cur)) {
            log.warn("[VisitClinicalStatus] visitId={} 이미 BILLED 상태 — BILLING_FAILED 무시", visitId);
            return;
        }
        vcs.setClinicalStatus("BILLING_FAILED");
        vcs.setUpdatedAt(LocalDateTime.now());
        log.warn("[VisitClinicalStatus] visitId={} {} → BILLING_FAILED (담당자 수동 재처리 필요)", visitId, cur);
    }

    // ─── 내부 유틸 ────────────────────────────────────────────

    private void transition(Long visitId, String expected, String next) {
        VisitClinicalStatus vcs = initOrGet(visitId);
        String cur = vcs.getClinicalStatus();
        if (!expected.equalsIgnoreCase(cur)) {
            log.warn("[VisitClinicalStatus] visitId={} 전이 불가: expected={}, actual={}", visitId, expected, cur);
            return;
        }
        vcs.setClinicalStatus(next);
        vcs.setUpdatedAt(LocalDateTime.now());
        log.info("[VisitClinicalStatus] visitId={} {} → {}", visitId, cur, next);
    }

    // [MODIFIED] BILLING_FAILED 추가 — isAfterOrEqual 비교 시 indexOf=-1 오작동 방지
    private static final java.util.List<String> ORDER = java.util.List.of(
            "SOAP_IN_PROGRESS", "EXAM_REQUESTED", "EXAM_IN_PROGRESS",
            "FINAL_ORDER_READY", "FINAL_ORDER_CONFIRMED", "BILLABLE", "BILLED", "BILLING_FAILED"
    );

    private boolean isAfterOrEqual(String cur, String target) {
        int ci = ORDER.indexOf(cur.toUpperCase());
        int ti = ORDER.indexOf(target.toUpperCase());
        // [ADDED] indexOf=-1(미등록 상태)이면 비교 불가 → false 반환 (안전장치)
        if (ci < 0 || ti < 0) return false;
        return ci >= ti;
    }
}
