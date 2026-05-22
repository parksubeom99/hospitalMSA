package kr.co.seoulit.his.clinical.saga;

import kr.co.seoulit.his.clinical.domain.visitstatus.VisitClinicalStatusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * [A-3] BillingFailedCompensationHandler 단위 테스트.
 *
 * 검증 대상: BILLING_FAILED 수신 시 clinical 자체 상태(visit_clinical_status)만으로
 *           자동 복구 / 수동 개입을 분기하는 하이브리드 보상 로직.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BillingFailedCompensationHandler 단위 테스트 — A-3 하이브리드 보상")
class BillingFailedCompensationHandlerTest {

    @Mock VisitClinicalStatusService visitStatusSvc;

    @InjectMocks BillingFailedCompensationHandler handler;

    @Test
    @DisplayName("BILLABLE 상태 → 자동 복구 (markBillableForRecovery 호출, BILLING_FAILED 미전환)")
    void compensate_WhenBillable_ShouldAutoRecover() {
        // given
        given(visitStatusSvc.getStatus(12001L)).willReturn("BILLABLE");

        // when
        handler.compensate(12001L, "INSUFFICIENT_BALANCE");

        // then — 자동 복구 경로
        verify(visitStatusSvc, times(1)).markBillableForRecovery(12001L);
        verify(visitStatusSvc, never()).markBillingFailed(anyLong());
    }

    @Test
    @DisplayName("BILLABLE이 아닌 비정상 상태 → 수동 개입 (markBillingFailed 호출)")
    void compensate_WhenNotBillable_ShouldRequireManual() {
        // given
        given(visitStatusSvc.getStatus(12002L)).willReturn("FINAL_ORDER_CONFIRMED");

        // when
        handler.compensate(12002L, "UNKNOWN");

        // then — 수동 개입 경로
        verify(visitStatusSvc, times(1)).markBillingFailed(12002L);
        verify(visitStatusSvc, never()).markBillableForRecovery(anyLong());
    }

    @Test
    @DisplayName("이미 BILLED 상태 → 보상 무시 (어떤 상태 전환도 하지 않음)")
    void compensate_WhenBilled_ShouldSkip() {
        // given
        given(visitStatusSvc.getStatus(12003L)).willReturn("BILLED");

        // when
        handler.compensate(12003L, "LATE_EVENT");

        // then — 지연 이벤트로 간주, 무시
        verify(visitStatusSvc, never()).markBillableForRecovery(anyLong());
        verify(visitStatusSvc, never()).markBillingFailed(anyLong());
    }

    @Test
    @DisplayName("canAutoRecover — BILLABLE만 자동 복구 가능으로 판정")
    void canAutoRecover_OnlyBillableTrue() {
        assertThat(handler.canAutoRecover("BILLABLE")).isTrue();
        assertThat(handler.canAutoRecover("FINAL_ORDER_CONFIRMED")).isFalse();
        assertThat(handler.canAutoRecover("BILLED")).isFalse();
    }
}
