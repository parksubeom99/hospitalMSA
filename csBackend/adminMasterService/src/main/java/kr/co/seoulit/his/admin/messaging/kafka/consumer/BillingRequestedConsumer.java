package kr.co.seoulit.his.admin.messaging.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.BillingService;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.invoice.InvoiceCreateRequest;
import kr.co.seoulit.his.admin.messaging.kafka.ProcessedEvent;
import kr.co.seoulit.his.admin.messaging.kafka.ProcessedEventRepository;
import kr.co.seoulit.his.admin.messaging.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Phase 2: Clinical → AdminMaster BILLING_REQUESTED 이벤트 수신
 *
 * 처리 흐름:
 * 1. BILLING_REQUESTED 수신
 * 2. BillingService.createInvoice() 호출 → 청구서 생성
 * 3. 성공: BILLING_COMPLETED 이벤트 발행 (Outbox)
 * 4. 실패: BILLING_FAILED 이벤트 발행 (Outbox)
 *
 * 멱등 보장: ProcessedEvent로 중복 처리 방지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingRequestedConsumer {

    private final BillingService billingService;
    private final ProcessedEventRepository processedRepo;
    private final OutboxService outbox;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${kafka.topic.his-billing-completed:his.adminmaster.billing.completed}")
    private String topicBillingCompleted;

    @Value("${kafka.topic.his-billing-failed:his.adminmaster.billing.failed}")
    private String topicBillingFailed;

    @KafkaListener(topics = "${kafka.topic.his-billing-requested:his.clinical.billing.requested}",
                   groupId = "${spring.kafka.consumer.group-id:admin-service}")
    @Transactional
    public void onBillingRequested(String message) throws Exception {
        JsonNode root = om.readTree(message);

        String eventId = root.path("eventId").asText();
        if (eventId == null || eventId.isBlank()) return;
        if (processedRepo.existsByEventId(eventId)) {
            log.debug("[BillingRequested] 중복 이벤트 무시. eventId={}", eventId);
            return;
        }

        String eventType = root.path("eventType").asText();
        if (!"BILLING_REQUESTED".equalsIgnoreCase(eventType)) {
            processedRepo.save(ProcessedEvent.builder()
                    .eventId(eventId).processedAt(LocalDateTime.now()).build());
            return;
        }

        JsonNode payload = root.path("payload");
        long visitId = payload.path("visitId").asLong();

        try {
            // 청구서 생성 (기본 금액 0 → 실제 환경에서는 진료비 계산 로직 연동)
            // Phase 2 포트폴리오 수준에서는 visitId 기반 자동 생성으로 충분
            InvoiceCreateRequest req = new InvoiceCreateRequest(visitId, 0);
            var invoice = billingService.createInvoice(req);

            // BILLING_COMPLETED 발행
            outbox.record(
                    "BILLING_COMPLETED",
                    "INVOICE",
                    String.valueOf(invoice.invoiceId()),
                    String.valueOf(visitId),
                    topicBillingCompleted,
                    Map.of(
                            "visitId", visitId,
                            "invoiceId", invoice.invoiceId(),
                            "status", "COMPLETED",
                            "completedAt", LocalDateTime.now().toString()
                    )
            );
            log.info("[Phase2] BillingRequested 처리 완료 → BILLING_COMPLETED. visitId={}, invoiceId={}", visitId, invoice.invoiceId());

        } catch (Exception e) {
            log.error("[Phase2] BillingRequested 처리 실패. visitId={}", visitId, e);

            // BILLING_FAILED 발행
            try {
                outbox.record(
                        "BILLING_FAILED",
                        "VISIT",
                        String.valueOf(visitId),
                        String.valueOf(visitId),
                        topicBillingFailed,
                        Map.of(
                                "visitId", visitId,
                                "reason", e.getMessage() != null ? e.getMessage() : "UNKNOWN",
                                "failedAt", LocalDateTime.now().toString()
                        )
                );
            } catch (Exception ex) {
                log.warn("[Phase2] BILLING_FAILED 발행도 실패. visitId={}", visitId, ex);
            }
        }

        processedRepo.save(ProcessedEvent.builder()
                .eventId(eventId).processedAt(LocalDateTime.now()).build());
    }
}
