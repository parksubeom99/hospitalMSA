package kr.co.seoulit.his.clinical.messaging.kafka;

import kr.co.seoulit.his.clinical.domain.visitstatus.VisitClinicalStatusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * DiagnosticOrderCompletedConsumer 단위 테스트
 * (Saga 체인 — Support → Clinical 검사 완료 수신부)
 *
 * 검증 항목:
 *  ① 정상 DIAGNOSTIC_ORDER_COMPLETED → markFinalOrderReady(visitId) 호출 + ProcessedEvent 저장
 *  ② 동일 eventId 중복 수신 → 모든 처리 건너뜀 (멱등성)
 *
 * 특이사항:
 *  - 후속 Outbox 이벤트 발행 없음 (Saga 체인 종료가 아님 — 사용자 액션으로 다음 단계 진행)
 *  - OutboxService 의존성 없음
 *  - @Value 필드 없음 → ReflectionTestUtils 불필요
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DiagnosticOrderCompletedConsumer 단위 테스트")
class DiagnosticOrderCompletedConsumerTest {

    @Mock VisitClinicalStatusService visitStatusSvc;
    @Mock ProcessedEventRepository processedRepo;

    @InjectMocks
    DiagnosticOrderCompletedConsumer consumer;

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 ①: 정상 DIAGNOSTIC_ORDER_COMPLETED → markFinalOrderReady + save
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("정상 DIAGNOSTIC_ORDER_COMPLETED 수신 시 visit_clinical_status가 FINAL_ORDER_READY로 전환되고 ProcessedEvent가 저장된다")
    void onDiagnosticOrderCompleted_Normal_ShouldMarkFinalOrderReadyAndSave() throws Exception {
        // given
        String message = """
                {
                  "eventId": "evt-301",
                  "eventType": "DIAGNOSTIC_ORDER_COMPLETED",
                  "payload": {
                    "visitId": 12001,
                    "orderId": 21001
                  }
                }
                """;
        given(processedRepo.existsByEventId("evt-301")).willReturn(false);

        // when
        consumer.onDiagnosticOrderCompleted(message);

        // then — markFinalOrderReady(12001L) 1회 호출
        verify(visitStatusSvc, times(1)).markFinalOrderReady(12001L);
        // then — ProcessedEvent 저장 1회 (eventId 일치 검증)
        verify(processedRepo, times(1)).save(argThat(e -> "evt-301".equals(e.getEventId())));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 ②: 멱등성 — 동일 eventId 중복 수신 시 모든 처리 건너뜀
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("이미 처리된 eventId로 중복 수신 시 markFinalOrderReady/save 모두 건너뛴다 (멱등성)")
    void onDiagnosticOrderCompleted_DuplicateEvent_ShouldBeIgnored() throws Exception {
        // given — 이미 처리된 이벤트
        String message = """
                {
                  "eventId": "evt-301",
                  "eventType": "DIAGNOSTIC_ORDER_COMPLETED",
                  "payload": {
                    "visitId": 12001,
                    "orderId": 21001
                  }
                }
                """;
        given(processedRepo.existsByEventId("evt-301")).willReturn(true);

        // when
        consumer.onDiagnosticOrderCompleted(message);

        // then — 어떤 상태 전환도 발생 안 함
        verify(visitStatusSvc, never()).markFinalOrderReady(anyLong());
        // then — ProcessedEvent save 호출 안 됨 (existsByEventId만 호출)
        verify(processedRepo, never()).save(any());
    }
}
