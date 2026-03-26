package kr.co.seoulit.his.clinical.messaging.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.seoulit.his.clinical.domain.finalorder.FinalOrder;
import kr.co.seoulit.his.clinical.domain.finalorder.FinalOrderRepository;
import kr.co.seoulit.his.clinical.domain.visitstatus.VisitClinicalStatusService;
import kr.co.seoulit.his.clinical.messaging.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Phase 2: Support/Admin 후속 execution task 완료 이벤트 수신
 *
 * 처리 흐름:
 * 1. FinalOrder 상태 IN_PROGRESS / DONE 전이
 * 2. DONE 전환 시 visitId 기준 전체 FinalOrder 완료 여부 집계
 * 3. 모두 DONE → VisitClinicalStatus BILLABLE 전환 + BILLING_REQUESTED 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionEventConsumer {

    private final FinalOrderRepository finalOrderRepo;
    private final ProcessedEventRepository processedRepo;
    private final VisitClinicalStatusService visitStatusSvc;
    private final OutboxService outbox;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${kafka.topic.his-billing-requested:his.clinical.billing.requested}")
    private String topicBillingRequested;

    @KafkaListener(topics = "${kafka.topic.support-execution:support.execution.v1}")
    @Transactional
    public void onSupportExecution(String message) throws Exception {
        handleExecution(message, "support");
    }

    @KafkaListener(topics = "${kafka.topic.admin-execution:admin.execution.v1}")
    @Transactional
    public void onAdminExecution(String message) throws Exception {
        handleExecution(message, "admin");
    }

    private void handleExecution(String message, String source) throws Exception {
        JsonNode root = om.readTree(message);

        String eventId = root.path("eventId").asText();
        if (eventId == null || eventId.isBlank()) return;
        if (processedRepo.existsByEventId(eventId)) return;

        String eventType = root.path("eventType").asText();
        if (eventType == null) eventType = "";
        JsonNode payload = root.path("payload");
        long finalOrderId = payload.path("finalOrderId").asLong();

        // FinalOrder 상태 전이
        String next = resolveNextStatus(eventType);
        FinalOrder fo = null;
        if (next != null) {
            fo = finalOrderRepo.findById(finalOrderId).orElse(null);
            if (fo != null && !"DONE".equalsIgnoreCase(fo.getStatus())) {
                fo.setStatus(next);
                fo.setUpdatedAt(LocalDateTime.now());
            }
        }

        processedRepo.save(ProcessedEvent.builder()
                .eventId(eventId).processedAt(LocalDateTime.now()).build());

        log.info("Consumed execution event: source={}, eventId={}, finalOrderId={}, eventType={}, nextStatus={}",
                source, eventId, finalOrderId, eventType, next);

        // Phase 2: DONE 전환 시 visitId 기준 집계 → 전체 완료 시 BILLING_REQUESTED 발행
        if ("DONE".equals(next) && fo != null) {
            Long visitId = fo.getVisitId();
            tryTriggerBilling(visitId);
        }
    }

    /**
     * visitId 기준 모든 FinalOrder가 DONE인지 확인 후 BILLING_REQUESTED 발행
     * - FINALIZED(최종오더 확정) + 후속 DONE(execution 완료)이 모두 충족될 때만 발행
     * - OutboxService를 통해 Outbox에 기록 → 원자성 보장
     */
    private void tryTriggerBilling(Long visitId) {
        try {
            List<FinalOrder> orders = finalOrderRepo.findByVisitId(visitId);
            if (orders.isEmpty()) return;

            // 모든 FinalOrder가 DONE 또는 FINALIZED 상태인지 확인
            // (FINALIZED 후 execution DONE이 오면 해당 fo는 DONE으로 전환됨)
            boolean allDone = orders.stream()
                    .allMatch(o -> "DONE".equalsIgnoreCase(o.getStatus())
                            || "CANCELED".equalsIgnoreCase(o.getStatus()));

            if (!allDone) {
                log.debug("[Phase2] visitId={} 아직 미완료 FinalOrder 존재. billing 대기.", visitId);
                return;
            }

            // VisitClinicalStatus → BILLABLE 전환
            visitStatusSvc.markBillable(visitId);

            // BILLING_REQUESTED Outbox 기록
            long totalOrderCount = orders.stream()
                    .filter(o -> !"CANCELED".equalsIgnoreCase(o.getStatus()))
                    .count();

            outbox.record(
                    "BILLING_REQUESTED",
                    "VISIT",
                    String.valueOf(visitId),
                    String.valueOf(visitId),
                    topicBillingRequested,
                    Map.of(
                            "visitId", visitId,
                            "finalOrderCount", totalOrderCount,
                            "requestedAt", LocalDateTime.now().toString()
                    )
            );
            log.info("[Phase2] BILLING_REQUESTED published. visitId={}, orderCount={}", visitId, totalOrderCount);

        } catch (Exception e) {
            log.warn("[Phase2] BILLING_REQUESTED 발행 실패. visitId={}", visitId, e);
        }
    }

    private String resolveNextStatus(String eventType) {
        String upper = eventType.toUpperCase();
        if (upper.contains("STARTED") || upper.contains("SCHEDULED") || upper.contains("ADMITTED")) {
            return "IN_PROGRESS";
        } else if (upper.contains("COMPLETED") || upper.contains("DISCHARGED") || upper.contains("RECORDED")) {
            return "DONE";
        }
        return null; // TASK_CREATED 등은 상태 변경 없음
    }
}
