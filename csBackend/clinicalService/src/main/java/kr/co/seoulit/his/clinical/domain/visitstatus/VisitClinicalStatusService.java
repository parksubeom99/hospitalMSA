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

    private static final java.util.List<String> ORDER = java.util.List.of(
            "SOAP_IN_PROGRESS", "EXAM_REQUESTED", "EXAM_IN_PROGRESS",
            "FINAL_ORDER_READY", "FINAL_ORDER_CONFIRMED", "BILLABLE", "BILLED"
    );

    private boolean isAfterOrEqual(String cur, String target) {
        int ci = ORDER.indexOf(cur.toUpperCase());
        int ti = ORDER.indexOf(target.toUpperCase());
        return ci >= ti;
    }
}
