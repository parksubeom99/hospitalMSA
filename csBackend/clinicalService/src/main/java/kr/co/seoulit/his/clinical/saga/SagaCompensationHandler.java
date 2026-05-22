package kr.co.seoulit.his.clinical.saga;

/**
 * [A-3] Saga 보상(Compensation) 핸들러 — Choreography Saga의 보상 트랜잭션 추상화.
 *
 * 설계 원칙: Choreography Saga에서 각 서비스는 "자기 소유 데이터"만으로 보상을 판정한다.
 * clinicalService는 invoice(adminMasterService 소유)를 조회하지 않고,
 * 자신이 소유한 visit_clinical_status 상태만으로 자동/수동 보상을 분기한다.
 */
public interface SagaCompensationHandler {

    /** 현재 임상 상태 기준으로 자동 보상(복구) 가능 여부 판정. */
    boolean canAutoRecover(String currentClinicalStatus);

    /** 보상 실행 — 자동 복구 또는 수동 개입 상태로 전환. */
    void compensate(Long visitId, String reason);
}
