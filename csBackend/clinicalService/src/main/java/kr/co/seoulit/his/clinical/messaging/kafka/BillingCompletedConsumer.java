package kr.co.seoulit.his.clinical.messaging.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.seoulit.his.clinical.domain.visitstatus.VisitClinicalStatusService;
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
 * 실패(BILLING_FAILED):    VisitClinicalStatus → BILLING_FAILED [MODIFIED]
 *                          담당자 수동 재처리 대기 상태로 명시
 *                          (자동 보상 트랜잭션 없음 — 실제 HIS 운영 패턴)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingCompletedConsumer {

    private final VisitClinicalStatusService visitStatusSvc;
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

        // [MODIFIED] 로그만 남기던 것 → BILLING_FAILED 상태 명시 전환
        visitStatusSvc.markBillingFailed(visitId); // [ADDED]

        processedRepo.save(ProcessedEvent.builder()
                .eventId(eventId).processedAt(LocalDateTime.now()).build());
        log.error("[Phase2] BILLING_FAILED 수신 → BILLING_FAILED 상태 전환. visitId={}, reason={}", visitId, reason);
    }
}
