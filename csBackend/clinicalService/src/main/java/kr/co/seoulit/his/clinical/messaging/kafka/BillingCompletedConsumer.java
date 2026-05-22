package kr.co.seoulit.his.clinical.messaging.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.seoulit.his.clinical.domain.visitstatus.VisitClinicalStatusService;
import kr.co.seoulit.his.clinical.saga.BillingFailedCompensationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Phase 2: AdminMaster → Clinical BILLING_COMPLETED / BILLING_FAILED 이벤트 수신
 *
 * 성공(BILLING_COMPLETED): VisitClinicalStatus → BILLED
 * 실패(BILLING_FAILED):    [A-3] 하이브리드 보상 — BillingFailedCompensationHandler에 위임.
 *                          임상 상태가 BILLABLE이면 자동 복구(재청구 가능),
 *                          비정상 상태면 BILLING_FAILED(수동 개입).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingCompletedConsumer {

    private final VisitClinicalStatusService visitStatusSvc;
    private final BillingFailedCompensationHandler compensationHandler; // [A-3]
    private final ProcessedEventRepository processedRepo;
    private final ObjectMapper om = new ObjectMapper();

    @KafkaListener(topics = "${kafka.topic.his-billing-completed:his.adminmaster.billing.completed}",
                   groupId = "${spring.kafka.consumer.group-id:clinical-service}")
    @Transactional
    public void onBillingCompleted(String message) throws Exception {
        JsonNode root = om.readTree(message);
        String eventId = root.path("eventId").asText();
        if (eventId == null || eventId.isBlank()) return;
        if (processedRepo.existsByEventId(eventId)) return;

        // [ADDED] eventType 필터 — 다른 이벤트 타입 유입 시 무시 처리
        String eventType = root.path("eventType").asText();
        if (!"BILLING_COMPLETED".equalsIgnoreCase(eventType)) {
            log.debug("[BillingCompleted] 처리 대상 아님. eventType={}, eventId={}", eventType, eventId);
            processedRepo.save(ProcessedEvent.builder()
                    .eventId(eventId).processedAt(LocalDateTime.now()).build());
            return;
        }

        long visitId = root.path("payload").path("visitId").asLong();
        visitStatusSvc.markBilled(visitId);

        processedRepo.save(ProcessedEvent.builder()
                .eventId(eventId).processedAt(LocalDateTime.now()).build());
        log.info("[Phase2] BILLING_COMPLETED 수신 → BILLED. visitId={}", visitId);
    }

    @KafkaListener(topics = "${kafka.topic.his-billing-failed:his.adminmaster.billing.failed}",
                   groupId = "${spring.kafka.consumer.group-id:clinical-service}")
    @Transactional
    public void onBillingFailed(String message) throws Exception {
        JsonNode root = om.readTree(message);
        String eventId = root.path("eventId").asText();
        if (eventId == null || eventId.isBlank()) return;
        if (processedRepo.existsByEventId(eventId)) return;

        // [ADDED] eventType 필터
        String eventType = root.path("eventType").asText();
        if (!"BILLING_FAILED".equalsIgnoreCase(eventType)) {
            log.debug("[BillingFailed] 처리 대상 아님. eventType={}, eventId={}", eventType, eventId);
            processedRepo.save(ProcessedEvent.builder()
                    .eventId(eventId).processedAt(LocalDateTime.now()).build());
            return;
        }

        long visitId = root.path("payload").path("visitId").asLong();
        String reason = root.path("payload").path("reason").asText("UNKNOWN");

        // [A-3] 하이브리드 보상 — 자동 복구 / 수동 개입 분기를 핸들러에 위임
        compensationHandler.compensate(visitId, reason);

        processedRepo.save(ProcessedEvent.builder()
                .eventId(eventId).processedAt(LocalDateTime.now()).build());
        log.info("[Phase2][A-3] BILLING_FAILED 수신 → 하이브리드 보상 처리 완료. visitId={}, reason={}", visitId, reason);
    }
}
