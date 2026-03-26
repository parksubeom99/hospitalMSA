package kr.co.seoulit.his.clinical.domain.visitstatus;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Phase 2: visitId 기준 Clinical 진행 상태
 *
 * 상태 전이:
 * SOAP_IN_PROGRESS
 *   → EXAM_REQUESTED         (SOAP 저장 + 검사요청 완료 시)
 *   → EXAM_IN_PROGRESS       (Support worklist 생성 완료 시, DIAGNOSTIC_ORDER_SUBMITTED 발행)
 *   → FINAL_ORDER_READY      (DIAGNOSTIC_ORDER_COMPLETED 수신 시)
 *   → FINAL_ORDER_CONFIRMED  (의사 최종오더 확정 시, FINAL_ORDER_FINALIZED 발행)
 *   → BILLABLE               (Support+Admin 후속 task 완료 집계 후)
 *   → BILLED                 (BILLING_COMPLETED 수신 후)
 */
@Entity
@Table(name = "visit_clinical_status")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class VisitClinicalStatus {

    @Id
    private Long visitId;

    @Column(nullable = false, length = 40)
    private String clinicalStatus;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
