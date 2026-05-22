package kr.co.seoulit.his.clinical.saga;

import kr.co.seoulit.his.clinical.domain.visitstatus.VisitClinicalStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * [A-3] BILLING_FAILED 이벤트에 대한 하이브리드 보상 핸들러.
 *
 * 완전 자동 롤백은 의료 업무 현실과 맞지 않는다(청구 실패가 진료 취소를 의미하지 않음).
 * 따라서 조건부 자동 + 수동 하이브리드로 보상한다:
 *  - clinicalStatus == BILLABLE : 정상적으로 청구 가능 단계였으므로 자동 복구
 *                                 (BILLABLE 유지 → 재청구 가능)
 *  - clinicalStatus == BILLED   : 이미 완료 → 보상 무시 (지연 이벤트)
 *  - 그 외 비정상 상태           : 수동 개입 필요 → BILLING_FAILED 명시 전환
 *
 * 판정 근거는 모두 clinicalService 자체 데이터(visit_clinical_status)이며,
 * adminMasterService의 invoice를 조회하지 않는다 (MSA 서비스 경계 보존).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingFailedCompensationHandler implements SagaCompensationHandler {

    private final VisitClinicalStatusService visitStatusSvc;

    @Override
    public boolean canAutoRecover(String currentClinicalStatus) {
        // BILLABLE = 청구 요청 직전의 정상 상태 → 자동으로 재청구 가능 상태 복원 가능
        return "BILLABLE".equalsIgnoreCase(currentClinicalStatus);
    }

    @Override
    public void compensate(Long visitId, String reason) {
        String status = visitStatusSvc.getStatus(visitId);

        if ("BILLED".equalsIgnoreCase(status)) {
            log.warn("[A-3 Saga보상] visitId={} 이미 BILLED — 보상 무시 (지연 이벤트)", visitId);
            return;
        }

        if (canAutoRecover(status)) {
            // 자동 보상: 재청구 가능한 BILLABLE 상태로 복원
            visitStatusSvc.markBillableForRecovery(visitId);
            log.info("[A-3 Saga보상] visitId={} 자동 복구 — BILLABLE 유지(재청구 가능). reason={}",
                    visitId, reason);
        } else {
            // 수동 보상: 담당자 재처리 대기 상태로 명시 전환
            visitStatusSvc.markBillingFailed(visitId);
            log.warn("[A-3 Saga보상] visitId={} 수동 개입 필요 — BILLING_FAILED. status={}, reason={}",
                    visitId, status, reason);
        }
    }
}
