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
 * BillingCompletedConsumer 단위 테스트 (Saga 종료 지점 + 보상 양방향)
 *
 * 검증 항목:
 *  ① 정상 BILLING_COMPLETED → visitStatusSvc.markBilled(visitId) 호출 + ProcessedEvent 저장
 *  ② BILLING_COMPLETED 중복 수신 → 모든 처리 건너뜀 (멱등성)
 *  ③ 정상 BILLING_FAILED → visitStatusSvc.markBillingFailed(visitId) 호출 + ProcessedEvent 저장
 *  ④ BILLING_FAILED 중복 수신 → 모든 처리 건너뜀 (멱등성)
 *
 * 특이사항:
 *  - 한 클래스(BillingCompletedConsumer)에 @KafkaListener 두 개 (onBillingCompleted, onBillingFailed)
 *  - 두 메서드 모두 동일 의존성 (VisitClinicalStatusService + ProcessedEventRepository) 공유
 *  - @Value 필드 없음 → ReflectionTestUtils 불필요 (admin BillingRequestedConsumer와 다름)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BillingCompletedConsumer 단위 테스트")
class BillingCompletedConsumerTest {

    @Mock VisitClinicalStatusService visitStatusSvc;
    @Mock ProcessedEventRepository processedRepo;

    @InjectMocks
    BillingCompletedConsumer consumer;

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 ①: 정상 BILLING_COMPLETED → markBilled 호출 + ProcessedEvent 저장
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("정상 BILLING_COMPLETED 수신 시 visit_clinical_status가 BILLED로 전환되고 ProcessedEvent가 저장된다")
    void onBillingCompleted_Normal_ShouldMarkBilledAndSave() throws Exception {
        // given
        String message = """
                {
                  "eventId": "evt-201",
                  "eventType": "BILLING_COMPLETED",
                  "payload": {
                    "visitId": 12001
                  }
                }
                """;
        given(processedRepo.existsByEventId("evt-201")).willReturn(false);

        // when
        consumer.onBillingCompleted(message);

        // then — markBilled(12001) 1회 호출
        verify(visitStatusSvc, times(1)).markBilled(12001L);
        // then — markBillingFailed는 호출 안 됨
        verify(visitStatusSvc, never()).markBillingFailed(anyLong());
        // then — ProcessedEvent 저장 1회 (eventId 일치 검증)
        verify(processedRepo, times(1)).save(argThat(e -> "evt-201".equals(e.getEventId())));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 ②: BILLING_COMPLETED 중복 수신 → 멱등성 (모든 처리 건너뜀)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("이미 처리된 eventId로 BILLING_COMPLETED 중복 수신 시 markBilled/save 모두 건너뛴다 (멱등성)")
    void onBillingCompleted_DuplicateEvent_ShouldBeIgnored() throws Exception {
        // given — 이미 처리된 이벤트
        String message = """
                {
                  "eventId": "evt-201",
                  "eventType": "BILLING_COMPLETED",
                  "payload": {
                    "visitId": 12001
                  }
                }
                """;
        given(processedRepo.existsByEventId("evt-201")).willReturn(true);

        // when
        consumer.onBillingCompleted(message);

        // then — 어떤 상태 전환도 발생 안 함
        verify(visitStatusSvc, never()).markBilled(anyLong());
        verify(visitStatusSvc, never()).markBillingFailed(anyLong());
        // then — ProcessedEvent save 호출 안 됨 (existsByEventId만 호출)
        verify(processedRepo, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 ③: 정상 BILLING_FAILED → markBillingFailed 호출 + ProcessedEvent 저장
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("정상 BILLING_FAILED 수신 시 visit_clinical_status가 BILLING_FAILED 상태로 명시 전환되고 ProcessedEvent가 저장된다")
    void onBillingFailed_Normal_ShouldMarkBillingFailedAndSave() throws Exception {
        // given
        String message = """
                {
                  "eventId": "evt-202",
                  "eventType": "BILLING_FAILED",
                  "payload": {
                    "visitId": 12002,
                    "reason": "DB connection failed"
                  }
                }
                """;
        given(processedRepo.existsByEventId("evt-202")).willReturn(false);

        // when
        consumer.onBillingFailed(message);

        // then — markBillingFailed(12002) 1회 호출
        verify(visitStatusSvc, times(1)).markBillingFailed(12002L);
        // then — markBilled는 호출 안 됨 (FAILED는 BILLED로 가지 않음)
        verify(visitStatusSvc, never()).markBilled(anyLong());
        // then — ProcessedEvent 저장 1회
        verify(processedRepo, times(1)).save(argThat(e -> "evt-202".equals(e.getEventId())));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 ④: BILLING_FAILED 중복 수신 → 멱등성
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("이미 처리된 eventId로 BILLING_FAILED 중복 수신 시 markBillingFailed/save 모두 건너뛴다 (멱등성)")
    void onBillingFailed_DuplicateEvent_ShouldBeIgnored() throws Exception {
        // given — 이미 처리된 이벤트
        String message = """
                {
                  "eventId": "evt-202",
                  "eventType": "BILLING_FAILED",
                  "payload": {
                    "visitId": 12002,
                    "reason": "DB connection failed"
                  }
                }
                """;
        given(processedRepo.existsByEventId("evt-202")).willReturn(true);

        // when
        consumer.onBillingFailed(message);

        // then — 어떤 상태 전환도 발생 안 함
        verify(visitStatusSvc, never()).markBilled(anyLong());
        verify(visitStatusSvc, never()).markBillingFailed(anyLong());
        // then — ProcessedEvent save 호출 안 됨
        verify(processedRepo, never()).save(any());
    }
}
