package kr.co.seoulit.his.admin.messaging.kafka.consumer;

import kr.co.seoulit.his.admin.domain.frontoffice.billing.BillingService;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.invoice.InvoiceCreateRequest;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.invoice.InvoiceResponse;
import kr.co.seoulit.his.admin.messaging.kafka.ProcessedEventRepository;
import kr.co.seoulit.his.admin.messaging.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * BillingRequestedConsumer 단위 테스트 (Saga 수납 진입점)
 *
 * 검증 항목:
 *  ① 정상 BILLING_REQUESTED → Invoice 생성 + BILLING_COMPLETED Outbox 발행
 *  ② 동일 eventId 중복 수신 → 멱등성 보장 (Invoice/Outbox 모두 호출 안 됨)
 *  ③ BillingService 예외 → BILLING_FAILED Outbox 발행 + ProcessedEvent 저장
 *
 * 참고 패턴: VisitRegisteredConsumerTest, VisitServiceTest의 ReflectionTestUtils 주입
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BillingRequestedConsumer 단위 테스트")
class BillingRequestedConsumerTest {

    @Mock BillingService billingService;
    @Mock ProcessedEventRepository processedRepo;
    @Mock OutboxService outbox;

    @InjectMocks
    BillingRequestedConsumer consumer;

    // @Value 두 필드는 @InjectMocks가 채워주지 않으므로 ReflectionTestUtils로 명시 주입
    // (VisitServiceTest의 topicVisitRegistered 주입 패턴과 동일)
    private static final String TOPIC_COMPLETED = "his.adminmaster.billing.completed";
    private static final String TOPIC_FAILED = "his.adminmaster.billing.failed";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(consumer, "topicBillingCompleted", TOPIC_COMPLETED);
        ReflectionTestUtils.setField(consumer, "topicBillingFailed", TOPIC_FAILED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 ①: 정상 BILLING_REQUESTED 수신 → Invoice 생성 + BILLING_COMPLETED 발행
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("정상 BILLING_REQUESTED 수신 시 Invoice 생성 후 BILLING_COMPLETED 이벤트가 Outbox로 발행된다")
    void onBillingRequested_Normal_ShouldCreateInvoiceAndPublishCompleted() throws Exception {
        // given
        String message = """
                {
                  "eventId": "evt-101",
                  "eventType": "BILLING_REQUESTED",
                  "payload": {
                    "visitId": 12001
                  }
                }
                """;
        given(processedRepo.existsByEventId("evt-101")).willReturn(false);

        InvoiceResponse mockInvoice = new InvoiceResponse(
                5001L, 12001L, "ISSUED", 0L, null, null, null
        );
        given(billingService.createInvoice(any(InvoiceCreateRequest.class))).willReturn(mockInvoice);

        // when
        consumer.onBillingRequested(message);

        // then — Invoice 생성 1회
        verify(billingService, times(1)).createInvoice(any(InvoiceCreateRequest.class));

        // then — BILLING_COMPLETED Outbox 발행 1회 (정확한 인자 검증)
        verify(outbox, times(1)).record(
                eq("BILLING_COMPLETED"),
                eq("INVOICE"),
                eq("5001"),
                eq("12001"),
                eq(TOPIC_COMPLETED),
                anyMap()
        );

        // then — BILLING_FAILED는 호출 안 됨
        verify(outbox, never()).record(
                eq("BILLING_FAILED"),
                anyString(), anyString(), anyString(), anyString(), anyMap()
        );

        // then — ProcessedEvent 저장 1회
        verify(processedRepo, times(1)).save(argThat(e -> "evt-101".equals(e.getEventId())));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 ②: 멱등성 — 동일 eventId 중복 수신 시 모든 처리 건너뜀
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("이미 처리된 eventId로 중복 수신 시 Invoice 생성/Outbox 발행/ProcessedEvent 저장 모두 건너뛴다 (멱등성)")
    void onBillingRequested_DuplicateEvent_ShouldBeIgnored() throws Exception {
        // given — 이미 처리된 이벤트
        String message = """
                {
                  "eventId": "evt-101",
                  "eventType": "BILLING_REQUESTED",
                  "payload": {
                    "visitId": 12001
                  }
                }
                """;
        given(processedRepo.existsByEventId("evt-101")).willReturn(true);

        // when
        consumer.onBillingRequested(message);

        // then — Invoice 생성 호출 안 됨
        verify(billingService, never()).createInvoice(any());
        // then — Outbox 어떤 이벤트도 발행 안 됨 (overload 둘 다 포함)
        verifyNoInteractions(outbox);
        // then — ProcessedEvent save 호출 안 됨 (existsByEventId만 호출)
        verify(processedRepo, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 ③: 예외 처리 — BillingService 실패 시 BILLING_FAILED 발행 + ProcessedEvent 저장
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("BillingService 예외 발생 시 BILLING_FAILED 이벤트가 Outbox로 발행되고 ProcessedEvent가 저장된다 (Saga 보상)")
    void onBillingRequested_BillingServiceFails_ShouldPublishFailedAndSaveProcessedEvent() throws Exception {
        // given
        String message = """
                {
                  "eventId": "evt-102",
                  "eventType": "BILLING_REQUESTED",
                  "payload": {
                    "visitId": 12002
                  }
                }
                """;
        given(processedRepo.existsByEventId("evt-102")).willReturn(false);
        given(billingService.createInvoice(any(InvoiceCreateRequest.class)))
                .willThrow(new RuntimeException("DB connection failed"));

        // when
        consumer.onBillingRequested(message);

        // then — Invoice 생성은 시도됨 (예외 발생)
        verify(billingService, times(1)).createInvoice(any(InvoiceCreateRequest.class));

        // then — BILLING_COMPLETED는 호출 안 됨
        verify(outbox, never()).record(
                eq("BILLING_COMPLETED"),
                anyString(), anyString(), anyString(), anyString(), anyMap()
        );

        // then — BILLING_FAILED Outbox 발행 1회 (정확한 인자 검증)
        verify(outbox, times(1)).record(
                eq("BILLING_FAILED"),
                eq("VISIT"),
                eq("12002"),
                eq("12002"),
                eq(TOPIC_FAILED),
                anyMap()
        );

        // then — ProcessedEvent 저장 (catch 블록 이후에도 실행되는 것을 검증)
        verify(processedRepo, times(1)).save(argThat(e -> "evt-102".equals(e.getEventId())));
    }
}
